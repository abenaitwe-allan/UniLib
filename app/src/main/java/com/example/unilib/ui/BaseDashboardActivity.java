package com.example.unilib.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.unilib.R;
import com.example.unilib.data.AuthRepository;
import com.example.unilib.data.DocumentRepository;
import com.example.unilib.data.ResultCallback;
import com.example.unilib.model.Document;
import com.example.unilib.model.Role;
import com.example.unilib.model.User;
import com.example.unilib.ui.auth.LoginActivity;
import com.example.unilib.ui.common.Labels;
import com.example.unilib.ui.upload.UploadActivity;
import com.example.unilib.util.NotificationHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * The chrome shared by the president, lecturer and admin screens: the coloured
 * header with the account name, the log out button, and a check that the signed
 * in account really still holds the role this screen needs.
 *
 * <p>Every screen using it must have {@code @id/main} on its root view and must
 * include {@code @layout/part_dashboard_top} so the header and the log out
 * button are present.
 */
public abstract class BaseDashboardActivity extends AppCompatActivity {

    /** The layout that holds the screen content. */
    protected abstract int layoutResource();

    /** The one role that is allowed to open this screen. */
    protected abstract Role requiredRole();

    /** The title shown in the header. */
    protected abstract int titleResource();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(layoutResource());

        View root = findViewById(R.id.main);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        TextView title = findViewById(R.id.tvDashTitle);
        if (title != null) title.setText(titleResource());

        TextView who = findViewById(R.id.tvDashUser);
        if (who != null) {
            String name = getIntent().getStringExtra(RoleRouter.EXTRA_NAME);
            who.setText(getString(R.string.dash_signed_in, roleLabel(),
                    (name == null || name.isEmpty()) ? "" : name));
        }

        Button logout = findViewById(R.id.btnLogout);
        if (logout != null) logout.setOnClickListener(v -> askToLogout());

        Button upload = findViewById(R.id.btnUpload);
        if (upload != null) {
            upload.setOnClickListener(v ->
                    startActivity(new Intent(this, UploadActivity.class)));
        }

        verifyRole();
    }

    /** The role written out for the header, for example "President". */
    protected String roleLabel() {
        return Labels.role(this, requiredRole());
    }

    // ---------- Log out ----------

    private void askToLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_logout, (d, which) -> logout())
                .show();
    }

    /** Ends the session and goes back to the login screen. */
    protected void logout() {
        goToLogin(null);
    }

    /**
     * Checks the account in Firestore still holds the role this screen needs.
     * The role can be taken away while the app is open, and a student must not
     * be able to open an admin screen by starting the activity directly.
     */
    private void verifyRole() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            goToLogin(getString(R.string.err_session));
            return;
        }
        new AuthRepository(this).fetchProfile(firebaseUser.getUid(), new ResultCallback<User>() {
            @Override public void onSuccess(User user) {
                if (user.parsedRole() != requiredRole()) {
                    goToLogin(getString(R.string.err_not_allowed));
                }
            }
            @Override public void onError(String message) {
                // A network problem should not throw the user out. A closed
                // session (missing profile, deactivated, broken role) should.
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    goToLogin(message);
                } else {
                    showMessage(message);
                }
            }
        });
    }

    /** Sends the user back to the login screen, with an optional reason. */
    protected void goToLogin(String reason) {
        runOnUiThread(() -> {
            new AuthRepository(this).logout();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            if (reason != null) i.putExtra(LoginActivity.EXTRA_REASON, reason);
            startActivity(i);
            finish();
        });
    }

    /** Shows a short message at the bottom of the screen. */
    protected void showMessage(String message) {
        if (message == null) return;
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    // ---------- Reviewing ----------

    /** Approves a document. onDone runs when the change reached Firestore. */
    protected void approveDocument(DocumentRepository repo, Document document, Runnable onDone) {
        repo.review(document.getId(), true, null, new ResultCallback<Void>() {
            @Override public void onSuccess(Void result) {
                showMessage(getString(R.string.review_approved, document.getTitle()));
                NotificationHelper.showApprovedNotification(BaseDashboardActivity.this, document);
                if (onDone != null) onDone.run();
            }
            @Override public void onError(String message) {
                showMessage(message);
            }
        });
    }

    /** Asks why a document is rejected, then saves the answer. */
    protected void rejectDocument(DocumentRepository repo, Document document, Runnable onDone) {
        EditText input = new EditText(this);
        input.setHint(R.string.review_reason_hint);
        int pad = getResources().getDimensionPixelSize(R.dimen.spacing_m);
        input.setPadding(pad, pad, pad, pad);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.review_reject_title)
                .setView(input)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_reject, (d, which) -> {
                    String reason = input.getText().toString().trim();
                    repo.review(document.getId(), false, reason, new ResultCallback<Void>() {
                        @Override public void onSuccess(Void result) {
                            showMessage(getString(R.string.review_rejected, document.getTitle()));
                            if (onDone != null) onDone.run();
                        }
                        @Override public void onError(String message) {
                            showMessage(message);
                        }
                    });
                })
                .show();
    }
}
