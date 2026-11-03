package com.example.unilib.ui.student;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.unilib.R;
import com.example.unilib.data.AuthRepository;
import com.example.unilib.data.DocumentRepository;
import com.example.unilib.data.OfflineDocumentStore;
import com.example.unilib.data.ResultCallback;
import com.example.unilib.model.Document;
import com.example.unilib.model.User;
import com.example.unilib.network.NetworkUtils;
import com.example.unilib.ui.RoleRouter;
import com.example.unilib.ui.auth.LoginActivity;
import com.example.unilib.ui.common.DocumentActions;
import com.example.unilib.util.DocCheckWorker;
import com.example.unilib.util.NotificationHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class StudentHomeActivity extends AppCompatActivity {
    private final List<Document> onlineDocs = new ArrayList<>();
    private final List<Document> offlineDocs = new ArrayList<>();
    private DocumentListAdapter adapter;
    private DocumentRepository repo;
    private OfflineDocumentStore store;
    private EditText etSearch;
    private TextView tvEmpty;
    private TextView tvProgram;
    private Chip chipOffline;
    private ProgressBar progress;

    private String programId;
    private String programName;
    private boolean offlineOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        repo = new DocumentRepository(this);
        store = new OfflineDocumentStore(this);
        NotificationHelper.createNotificationChannel(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        String name = getIntent().getStringExtra(RoleRouter.EXTRA_NAME);
        programId = getIntent().getStringExtra(RoleRouter.EXTRA_PROGRAM_ID);
        programName = getIntent().getStringExtra(RoleRouter.EXTRA_PROGRAM_NAME);

        ((TextView) findViewById(R.id.tvWelcome))
                .setText(getString(R.string.welcome_user, name == null ? "" : name));
        findViewById(R.id.btnLogout).setOnClickListener(v -> askToLogout());

        etSearch = findViewById(R.id.etSearch);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvProgram = findViewById(R.id.tvProgram);
        chipOffline = findViewById(R.id.chipOffline);
        progress = findViewById(R.id.progressDocs);

        // ListView in portrait, GridView in landscape: both are AbsListView
        AbsListView list = findViewById(R.id.listDocs);
        adapter = new DocumentListAdapter(this, new DocumentListAdapter.Listener() {
            @Override public void onOpen(Document document) {
                DocumentActions.open(StudentHomeActivity.this, document, store);
            }

            @Override public void onSaveToggle(Document document) {
                toggleOffline(document);
            }
        });
        list.setAdapter(adapter);
        list.setEmptyView(tvEmpty);
        list.setOnItemClickListener((parent, view, position, id) ->
                DocumentActions.open(this, adapter.getItem(position), store));

        chipOffline.setOnCheckedChangeListener((button, checked) -> {
            offlineOnly = checked;
            if (checked) {
                loadOffline();
            } else {
                loadOnline();
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { applyFilter(); }
        });

        updateProgramLabel();
        scheduleBackgroundDocWorker();
        if ((programId == null || programId.isEmpty()) && FirebaseAuth.getInstance().getCurrentUser() != null) {
            loadProgramOfCurrentStudent();
        }
        loadOffline();
        loadOnline();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (offlineOnly) {
            loadOffline();
        } else {
            loadOnline();
        }
    }

    /** Reads the programme of the signed in student if the intent did not carry it. */
    private void loadProgramOfCurrentStudent() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        new AuthRepository(this).fetchProfile(user.getUid(), new ResultCallback<User>() {
            @Override public void onSuccess(User profile) {
                programId = profile.getProgramId();
                programName = profile.getProgramName();
                updateProgramLabel();
                applyFilter();
            }

            @Override public void onError(String message) {
                // Keep showing whatever we can. A network problem is not fatal here.
            }
        });
    }

    private void updateProgramLabel() {
        if (programName != null && !programName.trim().isEmpty()) {
            tvProgram.setText(getString(R.string.program_documents, programName));
        } else {
            tvProgram.setText(R.string.program_documents_unknown);
        }
    }

    /** The approved documents of this student's programme. */
    private void loadOnline() {
        if (!NetworkUtils.isOnline(this)) {
            chipOffline.setChecked(true);
            return;
        }
        progress.setVisibility(View.VISIBLE);
        repo.listApproved(new ResultCallback<List<Document>>() {
            @Override public void onSuccess(List<Document> result) {
                progress.setVisibility(View.GONE);
                onlineDocs.clear();
                onlineDocs.addAll(result);
                checkAndNotifyNewDocuments(result);
                applyFilter();
            }

            @Override public void onError(String message) {
                progress.setVisibility(View.GONE);
                Toast.makeText(StudentHomeActivity.this, message, Toast.LENGTH_LONG).show();
                if (!offlineDocs.isEmpty()) chipOffline.setChecked(true);
            }
        });
    }

    private void checkAndNotifyNewDocuments(List<Document> docs) {
        if (docs == null || docs.isEmpty()) return;
        SharedPreferences prefs = getSharedPreferences("unilib_notified_docs", MODE_PRIVATE);
        Set<String> notified = new HashSet<>(prefs.getStringSet("notified_ids", new HashSet<>()));
        boolean firstTime = notified.isEmpty();
        Set<String> updatedNotified = new HashSet<>(notified);

        for (Document d : docs) {
            if (d == null || d.getId() == null) continue;
            boolean programOk = (programId == null || programId.isEmpty())
                    || (d.getProgramId() != null && d.getProgramId().equalsIgnoreCase(programId))
                    || (d.getProgramName() != null && programName != null && d.getProgramName().equalsIgnoreCase(programName));

            if (programOk) {
                if (!notified.contains(d.getId())) {
                    if (!firstTime) {
                        NotificationHelper.showApprovedNotification(this, d);
                    }
                    updatedNotified.add(d.getId());
                }
            }
        }
        prefs.edit().putStringSet("notified_ids", updatedNotified).apply();
    }

    /** The documents saved on this phone. */
    private void loadOffline() {
        offlineDocs.clear();
        offlineDocs.addAll(store.list());
        applyFilter();
    }

    private void applyFilter() {
        String query = etSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<Document> source = offlineOnly ? offlineDocs : onlineDocs;
        List<Document> result = new ArrayList<>();
        for (Document d : source) {
            boolean programOk = (programId == null || programId.isEmpty())
                    || (d.getProgramId() != null && d.getProgramId().equalsIgnoreCase(programId))
                    || (d.getProgramName() != null && programName != null && d.getProgramName().equalsIgnoreCase(programName))
                    || offlineOnly;
            String code = d.getCourseCode() == null ? "" : d.getCourseCode();
            String docProgram = d.getProgramName() == null ? "" : d.getProgramName();
            boolean textOk = query.isEmpty()
                    || (d.getTitle() != null && d.getTitle().toLowerCase(Locale.ROOT).contains(query))
                    || code.toLowerCase(Locale.ROOT).contains(query)
                    || docProgram.toLowerCase(Locale.ROOT).contains(query);
            if (programOk && textOk) result.add(d);
        }
        adapter.setSavedIds(store.savedIds());
        adapter.submit(result);
        tvEmpty.setText(offlineOnly ? R.string.offline_empty : R.string.empty_list);
        chipOffline.setText(offlineDocs.isEmpty()
                ? getString(R.string.offline_filter)
                : getString(R.string.offline_filter_count, offlineDocs.size()));
    }

    /** Saves the PDF on the phone, or removes it again. */
    private void toggleOffline(Document document) {
        if (store.isSaved(document.getId())) {
            store.remove(document.getId());
            Toast.makeText(this, getString(R.string.offline_removed_toast, document.getTitle()),
                    Toast.LENGTH_SHORT).show();
            loadOffline();
            return;
        }
        Toast.makeText(this, getString(R.string.offline_downloading, document.getTitle()),
                Toast.LENGTH_SHORT).show();
        store.download(document, new ResultCallback<File>() {
            @Override public void onSuccess(File result) {
                Toast.makeText(StudentHomeActivity.this,
                        getString(R.string.offline_saved_toast, document.getTitle()),
                        Toast.LENGTH_SHORT).show();
                loadOffline();
            }

            @Override public void onError(String message) {
                Toast.makeText(StudentHomeActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void scheduleBackgroundDocWorker() {
        try {
            if (programId != null && !programId.isEmpty()) {
                getSharedPreferences("unilib_user_session", MODE_PRIVATE)
                        .edit()
                        .putString("program_id", programId)
                        .putString("program_name", programName == null ? "" : programName)
                        .apply();
            }

            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                    DocCheckWorker.class, 15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build();

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "unilib_doc_check_worker",
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
            );
        } catch (Exception ignored) { }
    }

    private void askToLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_logout, (d, which) -> logout())
                .show();
    }

    private void logout() {
        new AuthRepository(this).logout();
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
