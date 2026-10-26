package com.example.unilib.ui.auth;

import android.content.Intent;
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
import com.example.unilib.model.CourseUnit;
import com.example.unilib.model.Program;
import com.example.unilib.ui.RoleRouter;
import com.example.unilib.ui.common.ProgramCoursePicker;
import com.example.unilib.util.Validators;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {
    private AuthViewModel vm;
    private TextInputLayout tilName, tilEmail, tilPassword, tilConfirm;
    private TextInputEditText etName, etEmail, etPassword, etConfirm;
    private Spinner spCourse;
    private ProgramCoursePicker picker;
    private CheckBox cbAgree;
    private TextView tvCourseError, tvAgreeError;
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
        spCourse = findViewById(R.id.spCourse);
        cbAgree = findViewById(R.id.cbAgree);
        tvCourseError = findViewById(R.id.tvCourseError);
        tvAgreeError = findViewById(R.id.tvAgreeError);
        btnRegister = findViewById(R.id.btnRegister);
        progress = findViewById(R.id.progress);

        // The program decides which course units are offered.
        picker = new ProgramCoursePicker(findViewById(R.id.spProgram), spCourse,
                new CatalogLoader(this), false);
        picker.setOnChanged((program, courseUnit) -> {
            if (tvCourseError.getVisibility() == View.VISIBLE) {
                tvCourseError.setVisibility(View.GONE);
            }
        });
        picker.load();

        btnRegister.setOnClickListener(v -> attemptRegister());        findViewById(R.id.tvBackToLogin).setOnClickListener(v -> finish());
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

    private void attemptRegister() {
        String name = textOf(etName).trim();
        String email = textOf(etEmail).trim();
        String password = textOf(etPassword);
        String confirm = textOf(etConfirm);
        boolean valid = true;

        tilName.setError(null); tilEmail.setError(null);
        tilPassword.setError(null); tilConfirm.setError(null);
        tvCourseError.setVisibility(View.GONE);
        tvAgreeError.setVisibility(View.GONE);

        if (!Validators.isValidName(name)) {
            tilName.setError(getString(R.string.error_name_invalid)); valid = false;
        }
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_email_required)); valid = false;
        } else if (!Validators.isValidEmail(email)) {
            tilEmail.setError(getString(R.string.error_email_invalid)); valid = false;
        }
        if (!Validators.isValidPassword(password)) {
            tilPassword.setError(getString(R.string.error_password_short)); valid = false;
        } else if (!Validators.hasLetterAndDigit(password)) {
            tilPassword.setError(getString(R.string.error_password_weak)); valid = false;
        }
        if (!confirm.equals(password)) {
            tilConfirm.setError(getString(R.string.error_confirm_mismatch)); valid = false;
        }
        Program program = picker.selectedProgram();
        CourseUnit courseUnit = picker.selectedCourseUnit();
        if (program == null) {
            Snackbar.make(findViewById(android.R.id.content),
                    getString(R.string.error_program_required), Snackbar.LENGTH_LONG).show();
            valid = false;
        } else if (courseUnit == null) {
            tvCourseError.setVisibility(View.VISIBLE);
            valid = false;
        }
        if (!cbAgree.isChecked()) {
            tvAgreeError.setVisibility(View.VISIBLE); valid = false;
        }
        if (!valid) return;

        vm.register(name, email, password, program.getId(), program.getName(),
                courseUnit.getCode(), courseUnit.getTitle());
    }

    private String textOf(TextInputEditText field) {
        Editable e = field.getText();
        return e == null ? "" : e.toString();
    }
}