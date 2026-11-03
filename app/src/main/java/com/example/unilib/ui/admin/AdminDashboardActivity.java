package com.example.unilib.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.unilib.R;
import com.example.unilib.data.CatalogLoader;
import com.example.unilib.data.DocumentRepository;
import com.example.unilib.data.ResultCallback;
import com.example.unilib.data.UserRepository;
import com.example.unilib.model.Document;
import com.example.unilib.model.Program;
import com.example.unilib.model.Role;
import com.example.unilib.model.User;
import com.example.unilib.ui.BaseDashboardActivity;
import com.example.unilib.ui.common.DocumentActions;
import com.example.unilib.ui.common.Labels;
import com.example.unilib.ui.common.ReviewAdapter;
import com.example.unilib.ui.common.SpinnerBinder;
import com.example.unilib.ui.common.UserAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * The admin's screen: documents, catalog management, and member role filter.
 */
public class AdminDashboardActivity extends BaseDashboardActivity {

    private DocumentRepository documentRepo;
    private UserRepository userRepo;

    private ReviewAdapter reviewAdapter;
    private UserAdapter userAdapter;
    private ProgressBar progressPending;
    private ProgressBar progressUsers;
    private Spinner spMemberRole;

    private final List<User> allUsers = new ArrayList<>();
    private final List<Program> availablePrograms = new ArrayList<>();

    @Override protected int layoutResource() { return R.layout.activity_admin_dashboard; }

    @Override protected Role requiredRole() { return Role.ADMIN; }

    @Override protected int titleResource() { return R.string.dash_title_admin; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        documentRepo = new DocumentRepository(this);
        userRepo = new UserRepository(this);

        ListView pendingList = findViewById(R.id.listPending);
        TextView emptyPending = findViewById(R.id.tvEmptyPending);
        progressPending = findViewById(R.id.progressPending);

        reviewAdapter = new ReviewAdapter(this, new ReviewAdapter.OnReview() {
            @Override public void onApprove(Document document) {
                approveDocument(documentRepo, document, AdminDashboardActivity.this::loadPending);
            }
            @Override public void onReject(Document document) {
                rejectDocument(documentRepo, document, AdminDashboardActivity.this::loadPending);
            }
        });
        pendingList.setAdapter(reviewAdapter);
        pendingList.setEmptyView(emptyPending);

        ListView usersList = findViewById(R.id.listUsers);
        TextView emptyUsers = findViewById(R.id.tvEmptyUsers);
        progressUsers = findViewById(R.id.progressUsers);
        spMemberRole = findViewById(R.id.spMemberRole);

        userAdapter = new UserAdapter(this, this::showMemberDialog);
        usersList.setAdapter(userAdapter);
        usersList.setEmptyView(emptyUsers);

        setupMemberRoleSpinner();

        findViewById(R.id.btnViewDocs).setOnClickListener(v -> showProgramSelectionDialog());
        findViewById(R.id.btnAddAccount).setOnClickListener(v ->
                startActivity(new Intent(this, AddAccountActivity.class)));
        findViewById(R.id.btnCatalog).setOnClickListener(v ->
                startActivity(new Intent(this, ProgramManagerActivity.class)));

        loadPrograms();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPending();
        loadUsers();
    }

    private void setupMemberRoleSpinner() {
        List<String> filters = new ArrayList<>();
        filters.add(getString(R.string.filter_members_all));
        filters.add(getString(R.string.filter_members_students));
        filters.add(getString(R.string.filter_members_presidents));
        filters.add(getString(R.string.filter_members_lecturers));
        filters.add(getString(R.string.filter_members_admins));

        SpinnerBinder.bind(spMemberRole, filters);
        spMemberRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyMemberFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void loadPrograms() {
        new CatalogLoader(this).loadPrograms(new ResultCallback<List<Program>>() {
            @Override
            public void onSuccess(List<Program> result) {
                availablePrograms.clear();
                availablePrograms.addAll(result);
            }

            @Override
            public void onError(String message) {
                // Non-fatal if programs cannot be preloaded
            }
        });
    }

    /** Prompts Admin to select a Program, then displays all documents under it. */
    private void showProgramSelectionDialog() {
        if (availablePrograms.isEmpty()) {
            new CatalogLoader(this).loadPrograms(new ResultCallback<List<Program>>() {
                @Override
                public void onSuccess(List<Program> result) {
                    availablePrograms.clear();
                    availablePrograms.addAll(result);
                    displayProgramPickerDialog();
                }

                @Override
                public void onError(String message) {
                    showMessage(message);
                }
            });
        } else {
            displayProgramPickerDialog();
        }
    }

    private void displayProgramPickerDialog() {
        if (availablePrograms.isEmpty()) {
            showMessage(getString(R.string.catalog_no_programs));
            return;
        }

        List<String> names = new ArrayList<>();
        names.add(getString(R.string.all_programs));
        for (Program p : availablePrograms) names.add(p.getName());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.admin_select_program)
                .setItems(names.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) {
                        showProgramDocuments(null, getString(R.string.all_programs));
                    } else {
                        Program p = availablePrograms.get(which - 1);
                        showProgramDocuments(p.getId(), p.getName());
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    /** Opens a dialog/list displaying documents under the selected program with a Delete option. */
    private void showProgramDocuments(String programId, String programName) {
        documentRepo.listByProgram(programId, programName, new ResultCallback<List<Document>>() {
            @Override
            public void onSuccess(List<Document> documents) {
                if (documents.isEmpty()) {
                    showMessage(getString(R.string.empty_list));
                    return;
                }

                ListView listView = new ListView(AdminDashboardActivity.this);
                AdminDocumentAdapter docAdapter = new AdminDocumentAdapter(AdminDashboardActivity.this, doc -> askToDeleteDocument(doc, () -> showProgramDocuments(programId, programName)));
                docAdapter.submit(documents);
                listView.setAdapter(docAdapter);

                listView.setOnItemClickListener((parent, view, position, id) -> {
                    Document doc = docAdapter.getItem(position);
                    DocumentActions.open(AdminDashboardActivity.this, doc);
                });

                new MaterialAlertDialogBuilder(AdminDashboardActivity.this)
                        .setTitle(programName)
                        .setView(listView)
                        .setPositiveButton(R.string.btn_cancel, null)
                        .show();
            }

            @Override
            public void onError(String message) {
                showMessage(message);
            }
        });
    }

    private void askToDeleteDocument(Document document, Runnable onDeleted) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.admin_confirm_delete_title)
                .setMessage(getString(R.string.admin_confirm_delete_msg, document.getTitle()))
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.admin_delete_doc, (dialog, which) -> {
                    documentRepo.deleteDocument(document.getId(), new ResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            showMessage(getString(R.string.admin_doc_deleted, document.getTitle()));
                            loadPending();
                            if (onDeleted != null) onDeleted.run();
                        }

                        @Override
                        public void onError(String message) {
                            showMessage(message);
                        }
                    });
                })
                .show();
    }

    /** Reads the documents that are still waiting for a decision. */
    private void loadPending() {
        progressPending.setVisibility(View.VISIBLE);
        documentRepo.listPending(new ResultCallback<List<Document>>() {
            @Override public void onSuccess(List<Document> documents) {
                progressPending.setVisibility(View.GONE);
                reviewAdapter.submit(documents);
            }
            @Override public void onError(String message) {
                progressPending.setVisibility(View.GONE);
                showMessage(message);
            }
        });
    }

    /** Reads the members list. */
    private void loadUsers() {
        progressUsers.setVisibility(View.VISIBLE);
        userRepo.listUsers(new ResultCallback<List<User>>() {
            @Override public void onSuccess(List<User> users) {
                progressUsers.setVisibility(View.GONE);
                allUsers.clear();
                allUsers.addAll(users);
                applyMemberFilter();
            }
            @Override public void onError(String message) {
                progressUsers.setVisibility(View.GONE);
                showMessage(message);
            }
        });
    }

    private void applyMemberFilter() {
        int pos = spMemberRole.getSelectedItemPosition();
        Role targetRole = null;
        if (pos == 1) targetRole = Role.STUDENT;
        else if (pos == 2) targetRole = Role.PRESIDENT;
        else if (pos == 3) targetRole = Role.LECTURER;
        else if (pos == 4) targetRole = Role.ADMIN;

        if (targetRole == null) {
            userAdapter.submit(allUsers);
            return;
        }

        List<User> filtered = new ArrayList<>();
        for (User u : allUsers) {
            if (u.parsedRole() == targetRole) {
                filtered.add(u);
            }
        }
        userAdapter.submit(filtered);
    }

    /** Lets the admin pick a new role for a member, or switch the account off. */
    private void showMemberDialog(User user) {
        List<String> options = new ArrayList<>();
        List<Role> roles = new ArrayList<>();
        for (Role role : Role.values()) {
            options.add(getString(R.string.admin_set_role, Labels.role(this, role)));
            roles.add(role);
        }
        options.add(user.isActive() ? getString(R.string.admin_deactivate) : getString(R.string.admin_activate));

        new MaterialAlertDialogBuilder(this)
                .setTitle(nameOf(user))
                .setItems(options.toArray(new String[0]), (d, which) -> {
                    if (which < roles.size()) {
                        changeRole(user, roles.get(which));
                    } else {
                        changeActive(user, !user.isActive());
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void changeRole(User user, Role role) {
        if (isMe(user)) {
            showMessage(getString(R.string.admin_cannot_change_own_role));
            return;
        }
        userRepo.updateRole(user.getId(), role, new ResultCallback<Void>() {
            @Override public void onSuccess(Void result) {
                showMessage(getString(R.string.admin_role_updated, nameOf(user),
                        Labels.role(AdminDashboardActivity.this, role)));
                loadUsers();
            }
            @Override public void onError(String message) {
                showMessage(message);
            }
        });
    }

    private void changeActive(User user, boolean active) {
        if (isMe(user)) {
            showMessage(getString(R.string.admin_cannot_change_own_role));
            return;
        }
        userRepo.setActive(user.getId(), active, new ResultCallback<Void>() {
            @Override public void onSuccess(Void result) {
                showMessage(getString(R.string.admin_member_updated, nameOf(user)));
                loadUsers();
            }
            @Override public void onError(String message) {
                showMessage(message);
            }
        });
    }

    private boolean isMe(User user) {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        return me != null && me.getUid().equals(user.getId());
    }

    private String nameOf(User user) {
        return user.getName() == null ? "" : user.getName();
    }
}
