package com.example.unilib.ui.common;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unilib.R;
import com.example.unilib.data.AuthRepository;
import com.example.unilib.data.CatalogLoader;
import com.example.unilib.data.ResultCallback;
import com.example.unilib.data.UserRepository;
import com.example.unilib.model.Program;
import com.example.unilib.model.Role;
import com.example.unilib.model.User;
import com.example.unilib.util.Validators;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for viewing and editing personal profile information.
 * Lecturers can select which programmes they teach.
 */
public class ProfileActivity extends AppCompatActivity {

    private AuthRepository authRepo;
    private UserRepository userRepo;
    private CatalogLoader catalogLoader;

    private TextInputLayout tilName, tilEmail;
    private TextInputEditText etName, etEmail;
    private TextView tvRole, tvProgram;
    private LinearLayout layoutLecturerSection, layoutTeachingCheckboxContainer;
    private Button btnSave;
    private ProgressBar progress;

    private User currentUser;
    private final List<CheckBox> teachingCheckBoxes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        authRepo = new AuthRepository(this);
        userRepo = new UserRepository(this);
        catalogLoader = new CatalogLoader(this);

        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        tvRole = findViewById(R.id.tvRole);
        tvProgram = findViewById(R.id.tvProgram);
        layoutLecturerSection = findViewById(R.id.layoutLecturerSection);
        layoutTeachingCheckboxContainer = findViewById(R.id.layoutTeachingCheckboxContainer);
        btnSave = findViewById(R.id.btnSave);
        progress = findViewById(R.id.progress);

        btnSave.setOnClickListener(v -> saveProfile());

        loadProfile();
    }

    private void loadProfile() {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) {
            finish();
            return;
        }

        setBusy(true);
        authRepo.fetchProfile(fUser.getUid(), new ResultCallback<User>() {
            @Override
            public void onSuccess(User user) {
                setBusy(false);
                currentUser = user;
                populateProfileViews();
            }

            @Override
            public void onError(String message) {
                setBusy(false);
                showMessage(message);
            }
        });
    }

    private void populateProfileViews() {
        if (currentUser == null) return;

        etName.setText(currentUser.getName() == null ? "" : currentUser.getName());
        etEmail.setText(currentUser.getEmail() == null ? "" : currentUser.getEmail());
        tvRole.setText(Labels.role(this, currentUser.parsedRole()));
        
        String progName = currentUser.getProgramName() == null || currentUser.getProgramName().trim().isEmpty()
                ? "General / All Programmes"
                : currentUser.getProgramName();
        tvProgram.setText(progName);

        if (currentUser.parsedRole() == Role.LECTURER) {
            layoutLecturerSection.setVisibility(View.VISIBLE);
            loadTeachingPrograms();
        } else {
            layoutLecturerSection.setVisibility(View.GONE);
        }
    }

    private void loadTeachingPrograms() {
        catalogLoader.loadPrograms(new ResultCallback<List<Program>>() {
            @Override
            public void onSuccess(List<Program> programs) {
                layoutTeachingCheckboxContainer.removeAllViews();
                teachingCheckBoxes.clear();

                List<String> currentTeaching = currentUser.getTeachingPrograms();
                if (currentTeaching == null) currentTeaching = new ArrayList<>();

                for (Program p : programs) {
                    CheckBox cb = new CheckBox(ProfileActivity.this);
                    cb.setText(p.getName());
                    cb.setTag(p.getName());
                    cb.setTextSize(14);
                    cb.setTextColor(getColor(R.color.unilib_text));
                    
                    if (currentTeaching.contains(p.getName())) {
                        cb.setChecked(true);
                    }
                    teachingCheckBoxes.add(cb);
                    layoutTeachingCheckboxContainer.addView(cb);
                }
            }

            @Override
            public void onError(String message) {
                // Ignore non-fatal error
            }
        });
    }

    private void saveProfile() {
        if (currentUser == null) return;

        String name = textOf(etName).trim();
        String email = textOf(etEmail).trim();

        tilName.setError(null);
        tilEmail.setError(null);

        if (!Validators.isValidName(name)) {
            tilName.setError(getString(R.string.error_name_invalid));
            return;
        }
        if (email.isEmpty() || !Validators.isValidEmail(email)) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            return;
        }

        List<String> selectedTeachingPrograms = new ArrayList<>();
        if (currentUser.parsedRole() == Role.LECTURER) {
            for (CheckBox cb : teachingCheckBoxes) {
                if (cb.isChecked()) {
                    selectedTeachingPrograms.add((String) cb.getTag());
                }
            }
        }

        setBusy(true);
        userRepo.updateProfile(currentUser.getId(), name, email, selectedTeachingPrograms, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                setBusy(false);
                Toast.makeText(ProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                setBusy(false);
                showMessage(message);
            }
        });
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!busy);
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
