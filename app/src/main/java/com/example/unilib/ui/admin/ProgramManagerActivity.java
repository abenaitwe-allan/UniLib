package com.example.unilib.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unilib.R;
import com.example.unilib.data.CatalogRepository;
import com.example.unilib.data.ResultCallback;
import com.example.unilib.model.CourseUnit;
import com.example.unilib.model.Program;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * The screen where the admin makes the programs and the course units under
 * them. Tapping or long-pressing any item allows the admin to delete it with validation.
 */
public class ProgramManagerActivity extends AppCompatActivity {

    private CatalogRepository catalog;

    private ProgramAdapter programAdapter;
    private CourseAdapter courseAdapter;
    private ProgressBar progressPrograms;
    private ProgressBar progressCourses;
    private TextView tvCourseHeader;

    /** The program whose course units are shown, or null when none is picked. */
    private Program selectedProgram;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_program_manager);

        catalog = new CatalogRepository(this);

        progressPrograms = findViewById(R.id.progressPrograms);
        progressCourses = findViewById(R.id.progressCourses);
        tvCourseHeader = findViewById(R.id.tvCourseHeader);

        ListView programsList = findViewById(R.id.listPrograms);
        programAdapter = new ProgramAdapter();
        programsList.setAdapter(programAdapter);
        programsList.setEmptyView(findViewById(R.id.tvEmptyPrograms));
        
        programsList.setOnItemClickListener((parent, view, position, id) -> {
            selectedProgram = programAdapter.getItem(position);
            loadCourseUnits();
        });

        programsList.setOnItemLongClickListener((parent, view, position, id) -> {
            Program program = programAdapter.getItem(position);
            askToDeleteProgram(program);
            return true;
        });

        ListView coursesList = findViewById(R.id.listCourses);
        courseAdapter = new CourseAdapter();
        coursesList.setAdapter(courseAdapter);
        coursesList.setEmptyView(findViewById(R.id.tvEmptyCourses));

        coursesList.setOnItemClickListener((parent, view, position, id) -> {
            CourseUnit unit = courseAdapter.getItem(position);
            askToDeleteCourseUnit(unit);
        });

        coursesList.setOnItemLongClickListener((parent, view, position, id) -> {
            CourseUnit unit = courseAdapter.getItem(position);
            askToDeleteCourseUnit(unit);
            return true;
        });

        findViewById(R.id.btnAddProgram).setOnClickListener(v -> askForProgram());
        findViewById(R.id.btnAddCourse).setOnClickListener(v -> askForCourseUnit());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPrograms();
    }

    /** Reads the programs the admin has made. */
    private void loadPrograms() {
        progressPrograms.setVisibility(View.VISIBLE);
        catalog.listPrograms(new ResultCallback<List<Program>>() {
            @Override public void onSuccess(List<Program> programs) {
                progressPrograms.setVisibility(View.GONE);
                programAdapter.submit(programs);
                if (selectedProgram != null) {
                    for (Program program : programs) {
                        if (program.getId() != null && program.getId().equals(selectedProgram.getId())) {
                            selectedProgram = program;
                            loadCourseUnits();
                            return;
                        }
                    }
                }
                if (!programs.isEmpty()) {
                    selectedProgram = programs.get(0);
                    loadCourseUnits();
                } else {
                    selectedProgram = null;
                    loadCourseUnits();
                }
            }
            @Override public void onError(String message) {
                progressPrograms.setVisibility(View.GONE);
                showMessage(message);
            }
        });
    }

    /** Reads the course units of the program that is picked. */
    private void loadCourseUnits() {
        if (selectedProgram == null) {
            courseAdapter.submit(new ArrayList<>());
            tvCourseHeader.setText(R.string.catalog_course_units);
            return;
        }
        tvCourseHeader.setText(getString(R.string.catalog_course_units)
                + " — " + selectedProgram.getName());
        progressCourses.setVisibility(View.VISIBLE);
        catalog.listCourseUnits(selectedProgram.getId(), new ResultCallback<List<CourseUnit>>() {
            @Override public void onSuccess(List<CourseUnit> units) {
                progressCourses.setVisibility(View.GONE);
                courseAdapter.submit(units);
            }
            @Override public void onError(String message) {
                progressCourses.setVisibility(View.GONE);
                showMessage(message);
            }
        });
    }

    /** Asks for validation confirmation before deleting a programme. */
    private void askToDeleteProgram(Program program) {
        if (program == null) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.admin_delete_program_title)
                .setMessage(getString(R.string.admin_delete_program_msg, program.getName()))
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.admin_delete_doc, (dialog, which) -> {
                    catalog.deleteProgram(program.getId(), new ResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            showMessage(getString(R.string.admin_program_deleted, program.getName()));
                            if (selectedProgram != null && selectedProgram.getId() != null
                                    && selectedProgram.getId().equals(program.getId())) {
                                selectedProgram = null;
                            }
                            loadPrograms();
                        }

                        @Override
                        public void onError(String message) {
                            showMessage(message);
                        }
                    });
                })
                .show();
    }

    /** Asks for validation confirmation before deleting a course unit. */
    private void askToDeleteCourseUnit(CourseUnit unit) {
        if (unit == null) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.admin_delete_course_title)
                .setMessage(getString(R.string.admin_delete_course_msg, unit.label()))
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.admin_delete_doc, (dialog, which) -> {
                    catalog.deleteCourseUnit(unit.getId(), new ResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            showMessage(getString(R.string.admin_course_deleted, unit.label()));
                            loadCourseUnits();
                        }

                        @Override
                        public void onError(String message) {
                            showMessage(message);
                        }
                    });
                })
                .show();
    }

    /** Asks for the name and the code of a new program. */
    private void askForProgram() {
        final EditText name = new EditText(this);
        final EditText code = new EditText(this);
        name.setHint(R.string.hint_program_name);
        code.setHint(R.string.hint_program_code);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = getResources().getDimensionPixelSize(R.dimen.spacing_m);
        name.setPadding(pad, pad, pad, pad);
        code.setPadding(pad, pad, pad, pad);
        box.addView(name);
        box.addView(code);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_add_program)
                .setView(box)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_add_program, (d, which) -> {
                    String programName = name.getText().toString().trim();
                    String programCode = code.getText().toString().trim().toUpperCase();
                    if (programName.isEmpty()) {
                        showMessage(getString(R.string.error_program_name_required));
                        return;
                    }
                    if (programCode.isEmpty()) {
                        showMessage(getString(R.string.error_program_code_required));
                        return;
                    }
                    addProgram(programName, programCode);
                })
                .show();
    }

    private void addProgram(String name, String code) {
        catalog.createProgram(name, code, new ResultCallback<Program>() {
            @Override public void onSuccess(Program program) {
                showMessage(getString(R.string.catalog_program_added, program.getName()));
                selectedProgram = program;
                loadPrograms();
            }
            @Override public void onError(String message) {
                showMessage(message);
            }
        });
    }

    /** Asks for the number and the name of a new course unit. */
    private void askForCourseUnit() {
        if (selectedProgram == null) {
            showMessage(getString(R.string.catalog_pick_program));
            return;
        }
        final EditText code = new EditText(this);
        final EditText title = new EditText(this);
        code.setHint(R.string.hint_course_code);
        title.setHint(R.string.hint_course_title);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = getResources().getDimensionPixelSize(R.dimen.spacing_m);
        code.setPadding(pad, pad, pad, pad);
        title.setPadding(pad, pad, pad, pad);
        box.addView(code);
        box.addView(title);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_add_course)
                .setView(box)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_add_course, (d, which) -> {
                    String courseCode = code.getText().toString().trim().toUpperCase();
                    String courseTitle = title.getText().toString().trim();
                    if (courseCode.isEmpty()) {
                        showMessage(getString(R.string.error_course_code_required));
                        return;
                    }
                    if (courseTitle.isEmpty()) {
                        showMessage(getString(R.string.error_course_title_required));
                        return;
                    }
                    addCourseUnit(courseCode, courseTitle);
                })
                .show();
    }

    private void addCourseUnit(String code, String title) {
        for (CourseUnit unit : courseAdapter.items) {
            if (unit.getCode() != null && unit.getCode().equalsIgnoreCase(code)) {
                showMessage(getString(R.string.error_course_code_taken));
                return;
            }
        }
        catalog.createCourseUnit(selectedProgram.getId(), code, title,
                new ResultCallback<CourseUnit>() {
                    @Override public void onSuccess(CourseUnit unit) {
                        showMessage(getString(R.string.catalog_course_added, unit.label()));
                        loadCourseUnits();
                    }
                    @Override public void onError(String message) {
                        showMessage(message);
                    }
                });
    }

    private void showMessage(String message) {
        if (message == null) return;
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    /** The list of programs. */
    private class ProgramAdapter extends BaseAdapter {
        private final List<Program> items = new ArrayList<>();

        void submit(List<Program> programs) {
            items.clear();
            items.addAll(programs);
            notifyDataSetChanged();
        }

        @Override public int getCount() { return items.size(); }
        @Override public Program getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = LayoutInflater.from(ProgramManagerActivity.this)
                        .inflate(R.layout.item_program, parent, false);
            }
            Program program = getItem(position);
            ((TextView) row.findViewById(R.id.tvProgramName)).setText(program.getName());
            ((TextView) row.findViewById(R.id.tvProgramCode)).setText(program.getCode());
            return row;
        }
    }

    /** The list of course units under the picked program. */
    private class CourseAdapter extends BaseAdapter {
        private final List<CourseUnit> items = new ArrayList<>();
        void submit(List<CourseUnit> units) {
            items.clear();
            items.addAll(units);
            notifyDataSetChanged();
        }

        @Override public int getCount() { return items.size(); }
        @Override public CourseUnit getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = LayoutInflater.from(ProgramManagerActivity.this)
                        .inflate(R.layout.item_course_unit, parent, false);
            }
            CourseUnit unit = getItem(position);
            ((TextView) row.findViewById(R.id.tvCourseCode)).setText(unit.getCode());
            ((TextView) row.findViewById(R.id.tvCourseTitle)).setText(unit.getTitle());
            return row;
        }
    }
}
