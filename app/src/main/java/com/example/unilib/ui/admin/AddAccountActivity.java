package com.example.unilib.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unilib.R;
import com.example.unilib.data.CatalogLoader;
import com.example.unilib.data.ResultCallback;
import com.example.unilib.data.UserRepository;
import com.example.unilib.model.Program;
import com.example.unilib.model.Role;
import com.example.unilib.model.User;
import com.example.unilib.ui.common.Labels;
import com.example.unilib.ui.common.SpinnerBinder;
import com.example.unilib.util.Validators;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * The screen where an admin makes an account for a lecturer or a class president.
 */
public class AddAccountActivity extends AppCompatActivity {

    private UserRepository users;
    private final List<Program> programs = new ArrayList<>();

    private TextInputLayout tilName, tilEmail, tilPassword;
    private TextInputEditText etName, etEmail, etPassword;
    private Spinner spRole, spProgram;
    private Button btnCreate;
    private ProgressBar progress;

    /** The roles an admin may hand out from this screen. */
    private final List<Role> roles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_account);

        users = new UserRepository(this);

        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        spRole = findViewById(R.id.spRole);
        spProgram = findViewById(R.id.spProgram);
        btnCreate = findViewById(R.id.btnCreate);
        progress = findViewById(R.id.progress);

        List<String> roleLabels = new ArrayList<>();
        roles.add(Role.LECTURER);
        roles.add(Role.PRESIDENT);
        for (Role role : roles) {
            roleLabels.add(Labels.role(this, role));
        }
        SpinnerBinder.bind(spRole, roleLabels);

        loadPrograms();

        btnCreate.setOnClickListener(v -> createAccount());
    }

    private void loadPrograms() {
        new CatalogLoader(this).loadPrograms(new ResultCallback<List<Program>>() {
            @Override public void onSuccess(List<Program> result) {
                programs.clear();
                programs.addAll(result);
                List<String> names = new ArrayList<>();
                names.add(getString(R.string.select_program));
                for (Program p : programs) names.add(p.getName());
                SpinnerBinder.bind(spProgram, names);
            }

            @Override public void onError(String message) {
                showMessage(message);
            }
        });
    }

    private Program selectedProgram() {
        int pos = spProgram.getSelectedItemPosition();
        if (pos <= 0 || pos - 1 >= programs.size()) return null;
        return programs.get(pos - 1);
    }

    private void createAccount() {
        String name = textOf(etName).trim();
        String email = textOf(etEmail).trim();
        String password = textOf(etPassword);

        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        boolean valid = true;

        if (!Validators.isValidName(name)) {
            tilName.setError(getString(R.string.error_name_invalid));
            valid = false;
        }
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_email_required));
            valid = false;
        } else if (!Validators.isValidEmail(email)) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            valid = false;
        }
        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.error_password_required));
            valid = false;
        } else if (!Validators.hasLetterAndDigit(password)) {
            tilPassword.setError(getString(R.string.error_password_weak));
            valid = false;
        }
        int rolePosition = spRole.getSelectedItemPosition();
        if (rolePosition < 0 || rolePosition >= roles.size()) {
            showMessage(getString(R.string.error_role_required));
            valid = false;
        }
        Program program = selectedProgram();
        if (program == null) {
            showMessage(getString(R.string.error_program_required));
            valid = false;
        }
        if (!valid) return;

        setBusy(true);
        users.createAccount(name, email, password, roles.get(rolePosition),
                program.getId(), program.getName(), "", "",
                new ResultCallback<User>() {
                    @Override public void onSuccess(User created) {
                        setBusy(false);
                        Toast.makeText(AddAccountActivity.this,
                                getString(R.string.add_account_created, name),
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                    @Override public void onError(String message) {
                        setBusy(false);
                        showMessage(message);
                    }
                });
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        btnCreate.setEnabled(!busy);
    }

    private void showMessage(String message) {
        if (message == null) return;
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    private String textOf(TextInputEditText field) {
        Editable e = field.getText();
        return e == null ? "" : e.toString();
    }
}
