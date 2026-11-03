package com.example.unilib.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.unilib.R;
import com.example.unilib.data.CatalogLoader;
import com.example.unilib.data.ResultCallback;
import com.example.unilib.model.Program;
import com.example.unilib.ui.RoleRouter;
import com.example.unilib.ui.common.SpinnerBinder;
import com.example.unilib.util.Validators;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {
    private AuthViewModel vm;
    private TextInputLayout tilName, tilEmail, tilPassword, tilConfirm;
    private TextInputEditText etName, etEmail, etPassword, etConfirm;
    private Spinner spProgram;
    private final List<Program> programs = new ArrayList<>();
    private CheckBox cbAgree;
    private TextView tvAgreeError;
    private Button btnRegister;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        vm = new ViewModelProvider(this).get(AuthViewModel.class);

        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirm = findViewById(R.id.tilConfirm);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);
        spProgram = findViewById(R.id.spProgram);
        cbAgree = findViewById(R.id.cbAgree);
        tvAgreeError = findViewById(R.id.tvAgreeError);
        btnRegister = findViewById(R.id.btnRegister);
        progress = findViewById(R.id.progress);

        // A student only chooses the programme. Every document under that
        // programme is visible on their dashboard, whatever the course unit.
        loadPrograms();

        btnRegister.setOnClickListener(v -> attemptRegister());
        findViewById(R.id.tvBackToLogin).setOnClickListener(v -> finish());
        cbAgree.setOnCheckedChangeListener((b, checked) -> {
            if (checked) tvAgreeError.setVisibility(View.GONE);
        });

        vm.getLoading().observe(this, isLoading -> {
            progress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnRegister.setEnabled(!isLoading);
        });
        vm.getUser().observe(this, user -> {
            if (user == null) return;
            Toast.makeText(this, getString(R.string.welcome_new, user.getName()),
                    Toast.LENGTH_LONG).show();
            RoleRouter.openHomeFor(this, user);
        });
        vm.getError().observe(this, message -> {
            if (message == null) return;
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
            vm.clearError();
        });
    }

    private void loadPrograms() {
        new CatalogLoader(this).loadPrograms(new ResultCallback<List<Program>>() {
            @Override public void onSuccess(List<Program> result) {
                programs.clear();
                programs.addAll(result);
                List<String> names = new ArrayList<>();
                names.add(getString(R.string.select_program));
                for (Program program : programs) names.add(program.getName());
                SpinnerBinder.bind(spProgram, names);
            }

            @Override public void onError(String message) {
                Snackbar.make(findViewById(android.R.id.content), message,
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private Program selectedProgram() {
        int pos = spProgram.getSelectedItemPosition();
        if (pos <= 0 || pos - 1 >= programs.size()) return null;
        return programs.get(pos - 1);
    }

    private void attemptRegister() {
        String name = textOf(etName).trim();
        String email = textOf(etEmail).trim();
        String password = textOf(etPassword);
        String confirm = textOf(etConfirm);
        boolean valid = true;

        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirm.setError(null);
        tvAgreeError.setVisibility(View.GONE);

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
        if (!Validators.isValidPassword(password)) {
            tilPassword.setError(getString(R.string.error_password_short));
            valid = false;
        } else if (!Validators.hasLetterAndDigit(password)) {
            tilPassword.setError(getString(R.string.error_password_weak));
            valid = false;
        }
        if (!confirm.equals(password)) {
            tilConfirm.setError(getString(R.string.error_confirm_mismatch));
            valid = false;
        }
        Program program = selectedProgram();
        if (program == null) {
            Snackbar.make(findViewById(android.R.id.content),
                    getString(R.string.error_program_required), Snackbar.LENGTH_LONG).show();
            valid = false;
        }
        if (!cbAgree.isChecked()) {
            tvAgreeError.setVisibility(View.VISIBLE);
            valid = false;
        }
        if (!valid) return;

        vm.register(name, email, password, program.getId(), program.getName());
    }

    private String textOf(TextInputEditText field) {
        Editable e = field.getText();
        return e == null ? "" : e.toString();
    }
}
