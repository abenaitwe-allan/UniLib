package com.example.unilib.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.unilib.R;
import com.example.unilib.data.DocumentRepository;
import com.example.unilib.data.ResultCallback;
import com.example.unilib.data.UserRepository;
import com.example.unilib.model.Document;
import com.example.unilib.model.Role;
import com.example.unilib.model.User;
import com.example.unilib.ui.BaseDashboardActivity;
import com.example.unilib.ui.common.Labels;
import com.example.unilib.ui.common.ReviewAdapter;
import com.example.unilib.ui.common.UserAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * The admin's screen: the documents waiting for a decision and the list of
 * members, where a role can be given or taken away.
 */
public class AdminDashboardActivity extends BaseDashboardActivity {

    private DocumentRepository documentRepo;
    private UserRepository userRepo;

    private ReviewAdapter reviewAdapter;
    private UserAdapter userAdapter;
    private ProgressBar progressPending;
    private ProgressBar progressUsers;

    @Override protected int layoutResource() { return R.layout.activity_admin_dashboard; }

    @Override protected Role requiredRole() { return Role.ADMIN; }

    @Override protected int titleResource() { return R.string.dash_title_admin; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Built here, not as a field: the context is not ready until now.
        documentRepo = new DocumentRepository(this);
        userRepo = new UserRepository(this);

        ListView pendingList = findViewById(R.id.listPending);
        TextView emptyPending = findViewById(R.id.tvEmptyPending);
        progressPending = findViewById(R.id.progressPending);

        reviewAdapter = new ReviewAdapter(this, myUid(), new ReviewAdapter.OnReview() {
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

        userAdapter = new UserAdapter(this, this::showMemberDialog);
        usersList.setAdapter(userAdapter);
        usersList.setEmptyView(emptyUsers);

        findViewById(R.id.btnAddAccount).setOnClickListener(v ->
                startActivity(new Intent(this, AddAccountActivity.class)));
        findViewById(R.id.btnCatalog).setOnClickListener(v ->
                startActivity(new Intent(this, ProgramManagerActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPending();
        loadUsers();
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
                userAdapter.submit(users);
            }
            @Override public void onError(String message) {
                progressUsers.setVisibility(View.GONE);
                showMessage(message);
            }
        });
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

    /** The id of the signed in admin, used to spot their own upload. */
    private String myUid() {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        return me == null ? null : me.getUid();
    }

    private String nameOf(User user) {
        return user.getName() == null ? "" : user.getName();
    }
}
