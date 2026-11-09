package com.example.unilib.ui.lecturer;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.unilib.R;
import com.example.unilib.data.AuthRepository;
import com.example.unilib.data.CatalogLoader;
import com.example.unilib.data.DocumentRepository;
import com.example.unilib.data.ResultCallback;
import com.example.unilib.model.CourseUnit;
import com.example.unilib.model.Document;
import com.example.unilib.model.Program;
import com.example.unilib.model.Role;
import com.example.unilib.model.User;
import com.example.unilib.ui.BaseDashboardActivity;
import com.example.unilib.ui.common.ReviewAdapter;
import com.example.unilib.ui.common.SpinnerBinder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * The lecturer's inbox: pending documents filtered by teaching programmes and course units.
 */
public class LecturerInboxActivity extends BaseDashboardActivity {

    private DocumentRepository repo;
    private AuthRepository authRepo;
    private CatalogLoader catalogLoader;

    private ReviewAdapter adapter;
    private ProgressBar progress;
    private Spinner spProgramFilter;
    private Spinner spCourseFilter;

    private final List<Document> allPendingDocs = new ArrayList<>();
    private final List<Program> lecturerPrograms = new ArrayList<>();
    private final List<CourseUnit> availableCourseUnits = new ArrayList<>();

    @Override protected int layoutResource() { return R.layout.activity_lecture_inbox; }

    @Override protected Role requiredRole() { return Role.LECTURER; }

    @Override protected int titleResource() { return R.string.dash_title_lecturer; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        repo = new DocumentRepository(this);
        authRepo = new AuthRepository(this);
        catalogLoader = new CatalogLoader(this);

        ListView list = findViewById(R.id.listPending);
        TextView empty = findViewById(R.id.tvEmpty);
        progress = findViewById(R.id.progress);
        spProgramFilter = findViewById(R.id.spProgramFilter);
        spCourseFilter = findViewById(R.id.spCourseFilter);

        adapter = new ReviewAdapter(this, new ReviewAdapter.OnReview() {
            @Override public void onApprove(Document document) {
                approveDocument(repo, document, LecturerInboxActivity.this::loadPending);
            }
            @Override public void onReject(Document document) {
                rejectDocument(repo, document, LecturerInboxActivity.this::loadPending);
            }
        });
        list.setAdapter(adapter);
        list.setEmptyView(empty);

        setupFilters();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLecturerProfileAndFilters();
        loadPending();
    }

    private void setupFilters() {
        spProgramFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0 || position - 1 >= lecturerPrograms.size()) {
                    loadCourseUnitsForProgram(null);
                } else {
                    Program p = lecturerPrograms.get(position - 1);
                    loadCourseUnitsForProgram(p);
                }
                applyFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        spCourseFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void loadLecturerProfileAndFilters() {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) return;

        authRepo.fetchProfile(fUser.getUid(), new ResultCallback<User>() {
            @Override
            public void onSuccess(User user) {
                List<String> teaching = user.getTeachingPrograms();
                catalogLoader.loadPrograms(new ResultCallback<List<Program>>() {
                    @Override
                    public void onSuccess(List<Program> allPrograms) {
                        lecturerPrograms.clear();
                        if (teaching != null && !teaching.isEmpty()) {
                            for (Program p : allPrograms) {
                                if (teaching.contains(p.getName())) {
                                    lecturerPrograms.add(p);
                                }
                            }
                        }
                        if (lecturerPrograms.isEmpty()) {
                            lecturerPrograms.addAll(allPrograms);
                        }

                        List<String> progNames = new ArrayList<>();
                        progNames.add(getString(R.string.all_programs));
                        for (Program p : lecturerPrograms) progNames.add(p.getName());
                        SpinnerBinder.bind(spProgramFilter, progNames);
                    }

                    @Override
                    public void onError(String message) { }
                });
            }

            @Override
            public void onError(String message) { }
        });
    }

    private void loadCourseUnitsForProgram(Program program) {
        if (program == null || program.getId() == null) {
            availableCourseUnits.clear();
            List<String> courseLabels = new ArrayList<>();
            courseLabels.add(getString(R.string.all_course_units));
            SpinnerBinder.bind(spCourseFilter, courseLabels);
            return;
        }

        catalogLoader.loadCourseUnits(program.getId(), new ResultCallback<List<CourseUnit>>() {
            @Override
            public void onSuccess(List<CourseUnit> units) {
                availableCourseUnits.clear();
                availableCourseUnits.addAll(units);

                List<String> courseLabels = new ArrayList<>();
                courseLabels.add(getString(R.string.all_course_units));
                for (CourseUnit u : availableCourseUnits) courseLabels.add(u.label());
                SpinnerBinder.bind(spCourseFilter, courseLabels);
            }

            @Override
            public void onError(String message) { }
        });
    }

    /** Reads the documents that are still waiting for a decision. */
    private void loadPending() {
        progress.setVisibility(View.VISIBLE);
        repo.listPending(new ResultCallback<List<Document>>() {
            @Override public void onSuccess(List<Document> documents) {
                progress.setVisibility(View.GONE);
                allPendingDocs.clear();
                allPendingDocs.addAll(documents);
                applyFilter();
            }
            @Override public void onError(String message) {
                progress.setVisibility(View.GONE);
                showMessage(message);
            }
        });
    }

    private void applyFilter() {
        int progPos = spProgramFilter.getSelectedItemPosition();
        Program selectedProgram = (progPos > 0 && progPos - 1 < lecturerPrograms.size())
                ? lecturerPrograms.get(progPos - 1) : null;

        int coursePos = spCourseFilter.getSelectedItemPosition();
        CourseUnit selectedCourse = (coursePos > 0 && coursePos - 1 < availableCourseUnits.size())
                ? availableCourseUnits.get(coursePos - 1) : null;

        List<Document> filtered = new ArrayList<>();
        for (Document d : allPendingDocs) {
            boolean progOk = selectedProgram == null
                    || (d.getProgramId() != null && d.getProgramId().equalsIgnoreCase(selectedProgram.getId()))
                    || (d.getProgramName() != null && d.getProgramName().equalsIgnoreCase(selectedProgram.getName()));

            boolean courseOk = selectedCourse == null
                    || (d.getCourseCode() != null && d.getCourseCode().equalsIgnoreCase(selectedCourse.getCode()));

            if (progOk && courseOk) {
                filtered.add(d);
            }
        }
        adapter.submit(filtered);
    }
}
