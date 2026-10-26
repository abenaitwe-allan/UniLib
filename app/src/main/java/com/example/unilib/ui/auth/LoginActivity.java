package com.example.unilib.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.unilib.R;
import com.example.unilib.ui.RoleRouter;
import com.example.unilib.util.Validators;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LIFECYCLE";
    private static final String KEY_ATTEMPTS = "failed_attempts";
    private static final String KEY_ROUTED = "already_routed";
    /** Told to the login screen by a screen that closed the session on purpose. */
    public static final String EXTRA_REASON = "extra_reason";

    private AuthViewModel vm;
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private CheckBox cbShowPassword;
    private Button btnLogin;
    private ProgressBar progress;
    private int failedAttempts = 0;
    /** Set once we have opened the home screen, so we never open it twice. */
    private boolean routed = false;

    // ---------- Lifecycle logging helper ----------
    private void log(String event) {
        Log.d(TAG, "Login." + event + " | activity#" + System.identityHashCode(this));
    }

    // ---------- Lifecycle callbacks ----------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        if (savedInstanceState != null) {
            failedAttempts = savedInstanceState.getInt(KEY_ATTEMPTS, 0);
            routed = savedInstanceState.getBoolean(KEY_ROUTED, false);
        }

        vm = new ViewModelProvider(this).get(AuthViewModel.class);
        log("onCreate (restoredState=" + (savedInstanceState != null)
                + ", viewModel#" + System.identityHashCode(vm)
                + ", failedAttempts=" + failedAttempts + ")");

        bindViews();
        setupEvents();
        observeViewModel();

        // Why are we back here? A screen may have closed the session on purpose.
        String reason = getIntent().getStringExtra(EXTRA_REASON);
        if (reason != null && !reason.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), reason, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override protected void onStart() {
        super.onStart();
        log("onStart");
        // Already signed in? Skip the form.
        if (!routed
                && FirebaseAuth.getInstance().getCurrentUser() != null
                && vm.getUser().getValue() == null) {
            vm.restoreSession();
        }
    }

    @Override protected void onRestoreInstanceState(@NonNull Bundle state) {
        super.onRestoreInstanceState(state);
        log("onRestoreInstanceState");
    }

    @Override protected void onResume()  { super.onResume();  log("onResume"); }
    @Override protected void onPause()   { super.onPause();   log("onPause"); }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ATTEMPTS, failedAttempts);
        outState.putBoolean(KEY_ROUTED, routed);
        log("onSaveInstanceState");
    }

    @Override protected void onStop()    { super.onStop();    log("onStop"); }
    @Override protected void onRestart() { super.onRestart(); log("onRestart"); }

    @Override protected void onDestroy() {
        super.onDestroy();
        log("onDestroy (isFinishing=" + isFinishing()
                + ", changingConfig=" + isChangingConfigurations() + ")");
    }

    // ---------- Setup ----------
    private void bindViews() {
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbShowPassword = findViewById(R.id.cbShowPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progress = findViewById(R.id.progress);
    }

    private void setupEvents() {
        // Action 1: tap Log in
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Action 2: press "Done" on the keyboard
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) { attemptLogin(); return true; }
            return false;
        });

        // Action 3: show or hide the password
        cbShowPassword.setOnCheckedChangeListener((button, checked) -> {
            etPassword.setTransformationMethod(checked
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            Editable text = etPassword.getText();
            if (text != null) etPassword.setSelection(text.length());
        });

        // Action 4: forgot password
        findViewById(R.id.tvForgot).setOnClickListener(v -> sendReset());

        // Action 5: go to Register
        findViewById(R.id.tvGoRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // Live feedback: clear an error as soon as the user edits the field
        etEmail.addTextChangedListener(new ClearErrorWatcher(tilEmail));
        etPassword.addTextChangedListener(new ClearErrorWatcher(tilPassword));
    }

    private void observeViewModel() {
        vm.getLoading().observe(this, isLoading -> {
            progress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnLogin.setEnabled(!isLoading);
        });

        vm.getUser().observe(this, user -> {
            if (user == null || routed) return;
            // Only ever open the home screen once. Without this the observer also
            // fires again after a rotation or a second tap, which stacks copies of
            // the dashboard on top of each other.
            routed = true;
            // Clear welcome feedback for a correct login
            Toast.makeText(this, getString(R.string.welcome_back, user.getName()),
                    Toast.LENGTH_LONG).show();
            RoleRouter.openHomeFor(this, user);
        });

        vm.getError().observe(this, message -> {
            if (message == null) return;
            failedAttempts++;
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
            vm.clearError();
        });

        vm.getInfo().observe(this, sentTo -> {
            if (sentTo == null) return;
            Snackbar.make(findViewById(android.R.id.content),
                    getString(R.string.reset_sent, sentTo), Snackbar.LENGTH_LONG).show();
            vm.clearInfo();
        });
    }

    // ---------- Validation and actions ----------
    private void attemptLogin() {
        String email = textOf(etEmail).trim();
        String password = textOf(etPassword);
        tilEmail.setError(null);
        tilPassword.setError(null);

        View firstInvalid = null;

        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_email_required));
            firstInvalid = etEmail;
        } else if (!Validators.isValidEmail(email)) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            firstInvalid = etEmail;
        }

        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.error_password_required));
            if (firstInvalid == null) firstInvalid = etPassword;
        } else if (!Validators.isValidPassword(password)) {
            tilPassword.setError(getString(R.string.error_password_short));
            if (firstInvalid == null) firstInvalid = etPassword;
        }

        if (firstInvalid != null) { firstInvalid.requestFocus(); return; }
        vm.login(email, password);
    }

    private void sendReset() {
        String email = textOf(etEmail).trim();
        if (!Validators.isValidEmail(email)) {
            tilEmail.setError(getString(R.string.reset_need_email));
            etEmail.requestFocus();
            return;
        }
        vm.sendPasswordReset(email);
    }

    private String textOf(TextInputEditText field) {
        Editable e = field.getText();
        return e == null ? "" : e.toString();
    }

    /** Clears a TextInputLayout error when the user types. */
    private static class ClearErrorWatcher implements TextWatcher {
        private final TextInputLayout layout;
        ClearErrorWatcher(TextInputLayout layout) { this.layout = layout; }
        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
        @Override public void onTextChanged(CharSequence s, int a, int b, int c) { layout.setError(null); }
        @Override public void afterTextChanged(Editable s) { }
    }
}