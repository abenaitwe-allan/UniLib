package com.example.unilib.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.unilib.R;
import com.example.unilib.data.AuthRepository;
import com.example.unilib.data.CatalogLoader;
import com.example.unilib.data.DocumentRepository;
import com.example.unilib.data.ResultCallback;
import com.example.unilib.model.CourseUnit;
import com.example.unilib.model.Document;
import com.example.unilib.model.Program;
import com.example.unilib.ui.RoleRouter;
import com.example.unilib.ui.auth.LoginActivity;
import com.example.unilib.ui.common.ProgramCoursePicker;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StudentHomeActivity extends AppCompatActivity {
    private final List<Document> allDocs = new ArrayList<>();
    private DocumentListAdapter adapter;
    private EditText etSearch;
    private ProgramCoursePicker picker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        String name = getIntent().getStringExtra(RoleRouter.EXTRA_NAME);
        ((TextView) findViewById(R.id.tvWelcome))
                .setText(getString(R.string.welcome_user, name == null ? "" : name));
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

        etSearch = findViewById(R.id.etSearch);
        TextView tvEmpty = findViewById(R.id.tvEmpty);

        // ListView in portrait, GridView in landscape: both are AbsListView
        AbsListView list = findViewById(R.id.listDocs);
        adapter = new DocumentListAdapter(this);
        list.setAdapter(adapter);
        list.setEmptyView(tvEmpty);

        // Action: tap an item
        list.setOnItemClickListener((parent, view, position, id) -> {
            Document d = adapter.getItem(position);
            Toast.makeText(this, getString(R.string.opening_doc, d.getTitle()),
                    Toast.LENGTH_SHORT).show();       // replaced by the PDF viewer in Step 11
        });
        // Action: long-press an item
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            Document d = adapter.getItem(position);
            Toast.makeText(this, getString(R.string.doc_details, d.getTitle(), d.getUploaderName()),
                    Toast.LENGTH_LONG).show();
            return true;
        });

        // The program decides which course units can be filtered by.
        picker = new ProgramCoursePicker(findViewById(R.id.spFilterProgram),
                findViewById(R.id.spFilterCourse), new CatalogLoader(this), true);
        picker.setOnChanged((program, courseUnit) -> applyFilter());
        picker.load();

        // The documents that are in the library, straight from Firestore.
        new DocumentRepository(this).listApproved(new ResultCallback<List<Document>>() {
            @Override public void onSuccess(List<Document> result) {
                allDocs.clear();
                allDocs.addAll(result);
                applyFilter();
            }

            @Override public void onError(String message) {
                Toast.makeText(StudentHomeActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
        // Action: type in search
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { applyFilter(); }
        });
    }

    private void applyFilter() {
        String query = etSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        Program program = picker == null ? null : picker.selectedProgram();
        CourseUnit courseUnit = picker == null ? null : picker.selectedCourseUnit();
        List<Document> result = new ArrayList<>();
        for (Document d : allDocs) {
            boolean programOk = program == null
                    || (d.getProgramId() != null && d.getProgramId().equals(program.getId()));
            boolean courseOk = courseUnit == null
                    || (d.getCourseCode() != null && d.getCourseCode().equals(courseUnit.getCode()));
            String code = d.getCourseCode() == null ? "" : d.getCourseCode();
            String programName = d.getProgramName() == null ? "" : d.getProgramName();
            boolean textOk = query.isEmpty()
                    || d.getTitle().toLowerCase(Locale.ROOT).contains(query)
                    || code.toLowerCase(Locale.ROOT).contains(query)
                    || programName.toLowerCase(Locale.ROOT).contains(query);
            if (programOk && courseOk && textOk) result.add(d);
        }
        adapter.submit(result);
    }

    private void logout() {
        new AuthRepository(this).logout();
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}