package com.example.unilib.ui.president;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.unilib.R;
import com.example.unilib.data.AuthRepository;
import com.example.unilib.data.DocumentRepository;
import com.example.unilib.data.OfflineDocumentStore;
import com.example.unilib.data.ResultCallback;
import com.example.unilib.model.Document;
import com.example.unilib.model.Role;
import com.example.unilib.model.User;
import com.example.unilib.network.NetworkUtils;
import com.example.unilib.ui.BaseDashboardActivity;
import com.example.unilib.ui.RoleRouter;
import com.example.unilib.ui.common.DocumentActions;
import com.example.unilib.ui.common.DocumentStatusAdapter;
import com.example.unilib.ui.student.DocumentListAdapter;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The class president's screen. A president is a student too, so the library
 * of approved documents is shown next to the uploads this member has shared.
 */
public class PresidentDashboardActivity extends BaseDashboardActivity {

    private DocumentRepository repo;
    private OfflineDocumentStore store;

    private final List<Document> library = new ArrayList<>();
    private final List<Document> uploads = new ArrayList<>();

    private ListView list;
    private TextView empty;
    private TextView tvSection;
    private ProgressBar progress;
    private DocumentListAdapter libraryAdapter;
    private DocumentStatusAdapter uploadAdapter;

    private String programId;
    private String programName;
    private boolean showLibrary = true;

    @Override protected int layoutResource() {
        return R.layout.activity_president_dashboard_activivty;
    }

    @Override protected Role requiredRole() { return Role.PRESIDENT; }

    @Override protected int titleResource() { return R.string.dash_title_president; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        repo = new DocumentRepository(this);
        store = new OfflineDocumentStore(this);
        programId = getIntent().getStringExtra(RoleRouter.EXTRA_PROGRAM_ID);
        programName = getIntent().getStringExtra(RoleRouter.EXTRA_PROGRAM_NAME);

        list = findViewById(R.id.listDocuments);
        empty = findViewById(R.id.tvEmpty);
        tvSection = findViewById(R.id.tvSection);
        progress = findViewById(R.id.progress);

        libraryAdapter = new DocumentListAdapter(this, new DocumentListAdapter.Listener() {
            @Override public void onOpen(Document document) {
                DocumentActions.open(PresidentDashboardActivity.this, document, store);
            }

            @Override public void onSaveToggle(Document document) {
                toggleOffline(document);
            }
        });
        uploadAdapter = new DocumentStatusAdapter(this);
        list.setEmptyView(empty);
        list.setOnItemClickListener((parent, view, position, id) -> {
            Document document = showLibrary
                    ? libraryAdapter.getItem(position)
                    : uploadAdapter.getItem(position);
            DocumentActions.open(this, document, store);
        });

        MaterialButtonToggleGroup toggle = findViewById(R.id.toggleSection);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            showLibrary = checkedId == R.id.btnTabLibrary;
            render();
        });

        if ((programId == null || programId.isEmpty())
                && FirebaseAuth.getInstance().getCurrentUser() != null) {
            loadProgramOfCurrentMember();
        }
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUploads();
        loadLibrary();
    }

    private void loadProgramOfCurrentMember() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        new AuthRepository(this).fetchProfile(user.getUid(), new ResultCallback<User>() {
            @Override public void onSuccess(User profile) {
                programId = profile.getProgramId();
                programName = profile.getProgramName();
                loadLibrary();
            }

            @Override public void onError(String message) {
                // The library still works; it will simply show every programme.
            }
        });
    }

    /** Everything this member has uploaded, with the review status. */
    private void loadUploads() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        repo.listByUploader(user.getUid(), new ResultCallback<List<Document>>() {
            @Override public void onSuccess(List<Document> documents) {
                uploads.clear();
                uploads.addAll(documents);
                if (!showLibrary) render();
            }

            @Override public void onError(String message) {
                showMessage(message);
            }
        });
    }

    /** Approved documents in this member's programme. */
    private void loadLibrary() {
        if (!NetworkUtils.isOnline(this)) {
            library.clear();
            library.addAll(store.list());
            render();
            return;
        }
        progress.setVisibility(View.VISIBLE);
        repo.listApproved(new ResultCallback<List<Document>>() {
            @Override public void onSuccess(List<Document> documents) {
                progress.setVisibility(View.GONE);
                library.clear();
                for (Document document : documents) {
                    boolean programOk = (programId == null || programId.isEmpty())
                            || (document.getProgramId() != null && document.getProgramId().equalsIgnoreCase(programId))
                            || (document.getProgramName() != null && programName != null && document.getProgramName().equalsIgnoreCase(programName));
                    if (programOk) {
                        library.add(document);
                    }
                }
                render();
            }

            @Override public void onError(String message) {
                progress.setVisibility(View.GONE);
                library.clear();
                library.addAll(store.list());
                render();
                showMessage(message);
            }
        });
    }

    private void render() {
        if (list == null) return;
        if (showLibrary) {
            list.setAdapter(libraryAdapter);
            libraryAdapter.setSavedIds(store.savedIds());
            libraryAdapter.submit(library);
            tvSection.setText(programName == null || programName.trim().isEmpty()
                    ? getString(R.string.president_tab_library)
                    : getString(R.string.president_library_program, programName));
            empty.setText(R.string.president_library_empty);
        } else {
            list.setAdapter(uploadAdapter);
            uploadAdapter.submit(uploads);
            tvSection.setText(R.string.president_my_uploads);
            empty.setText(R.string.president_no_uploads);
        }
    }

    private void toggleOffline(Document document) {
        if (store.isSaved(document.getId())) {
            store.remove(document.getId());
            Toast.makeText(this, getString(R.string.offline_removed_toast, document.getTitle()),
                    Toast.LENGTH_SHORT).show();
            render();
            return;
        }
        Toast.makeText(this, getString(R.string.offline_downloading, document.getTitle()),
                Toast.LENGTH_SHORT).show();
        store.download(document, new ResultCallback<File>() {
            @Override public void onSuccess(File result) {
                Toast.makeText(PresidentDashboardActivity.this,
                        getString(R.string.offline_saved_toast, document.getTitle()),
                        Toast.LENGTH_SHORT).show();
                render();
            }

            @Override public void onError(String message) {
                Toast.makeText(PresidentDashboardActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
