package com.example.unilib.ui.common;

import android.widget.AdapterView;
import android.widget.Spinner;

import com.example.unilib.R;
import com.example.unilib.data.CatalogLoader;
import com.example.unilib.data.ResultCallback;
import com.example.unilib.model.CourseUnit;
import com.example.unilib.model.Program;

import java.util.ArrayList;
import java.util.List;

/**
 * Drives the two dropdowns that always go together: the program, and the
 * course units that belong to the chosen program.
 *
 * <p>Picking a program reloads the course units. The course units of the
 * program the admin made are the only ones shown, which is the whole point of
 * the two dropdowns.
 *
 * <p>With {@code withAllOption} the first entry means "no filter" and the
 * course dropdown is switched off until a program is picked. Without it the
 * first entry is a prompt to choose, and both dropdowns have to be filled in
 * before the form can be sent.
 */
public class ProgramCoursePicker {

    /** Told whenever the user changes either dropdown. */
    public interface OnChanged {
        void onChanged(Program program, CourseUnit courseUnit);
    }

    private final Spinner programSpinner;
    private final Spinner courseSpinner;
    private final CatalogLoader loader;
    private final boolean withAllOption;

    private final List<Program> programs = new ArrayList<>();
    private final List<CourseUnit> courseUnits = new ArrayList<>();

    /** Set while the dropdowns are being filled, to ignore the callbacks it fires. */
    private boolean filling = false;
    private OnChanged listener;

    public ProgramCoursePicker(Spinner programSpinner, Spinner courseSpinner,
                               CatalogLoader loader, boolean withAllOption) {
        this.programSpinner = programSpinner;
        this.courseSpinner = courseSpinner;
        this.loader = loader;
        this.withAllOption = withAllOption;
    }

    public void setOnChanged(OnChanged listener) {
        this.listener = listener;
    }

    /** Reads the catalogue and fills the program dropdown. */
    public void load() {
        loader.loadPrograms(new ResultCallback<List<Program>>() {
            @Override public void onSuccess(List<Program> result) {
                programs.clear();
                programs.addAll(result);
                fillProgramDropdown();
                // The course units of the first program are shown straight away,
                // so the second dropdown is never empty for no reason.
                if (!withAllOption && !programs.isEmpty()) {
                    selectProgram(0);
                } else {
                    fillCourseDropdown(null);
                }            }

            @Override public void onError(String message) {
                // CatalogLoader falls back to the built in list, so this only
                // happens if even that fails.
                fillProgramDropdown();
                fillCourseDropdown(null);
            }
        });
    }

    private void fillProgramDropdown() {
        List<String> names = new ArrayList<>();
        names.add(programSpinner.getContext().getString(
                withAllOption ? R.string.all_programs : R.string.select_program));
        for (Program program : programs) {
            names.add(program.getName());
        }
        filling = true;
        SpinnerBinder.bind(programSpinner, names);
        filling = false;

        programSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, android.view.View v, int pos, long id) {
                if (filling) return;
                if (pos == 0) {
                    fillCourseDropdown(null);
                } else {
                    loadCourseUnits(programs.get(pos - 1));
                }
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });
    }

    private void loadCourseUnits(Program program) {
        loader.loadCourseUnits(program.getId(), new ResultCallback<List<CourseUnit>>() {
            @Override public void onSuccess(List<CourseUnit> result) {
                courseUnits.clear();
                courseUnits.addAll(result);
                fillCourseDropdown(program);
            }
            @Override public void onError(String message) {
                courseUnits.clear();
                fillCourseDropdown(program);
            }
        });
    }

    private void fillCourseDropdown(Program program) {
        List<String> labels = new ArrayList<>();
        labels.add(courseSpinner.getContext().getString(
                withAllOption ? R.string.all_course_units : R.string.select_course_unit));
        for (CourseUnit unit : courseUnits) {
            labels.add(unit.label());
        }
        filling = true;
        SpinnerBinder.bind(courseSpinner, labels);
        filling = false;
        // The dropdown stays usable even when there is nothing to pick from,
        // so its text keeps full contrast and the user can see what is asked.
        courseSpinner.setEnabled(true);
        notifyChanged();
    }

    /**
     * Picks a program. The number is the place in the program list, so the
     * first program is 0 and lands on the first row after the prompt.
     */
    private void selectProgram(int programIndex) {
        if (programIndex < 0 || programIndex >= programs.size()) return;
        filling = true;
        programSpinner.setSelection(programIndex + 1);
        filling = false;
        loadCourseUnits(programs.get(programIndex));
    }

    private void notifyChanged() {
        if (listener != null) listener.onChanged(selectedProgram(), selectedCourseUnit());
    }

    /** The program the user picked, or null when none was picked. */
    public Program selectedProgram() {
        int pos = programSpinner.getSelectedItemPosition();
        if (pos <= 0 || pos - 1 >= programs.size()) return null;
        return programs.get(pos - 1);
    }

    /** The course unit the user picked, or null when none was picked. */
    public CourseUnit selectedCourseUnit() {
        int pos = courseSpinner.getSelectedItemPosition();
        if (pos <= 0 || pos - 1 >= courseUnits.size()) return null;
        return courseUnits.get(pos - 1);
    }

    /** True when the user has picked both a program and a course unit. */
    public boolean isComplete() {
        return selectedProgram() != null && selectedCourseUnit() != null;
    }
}
