package com.example.unilib.data;

import android.content.Context;

import com.example.unilib.R;
import com.example.unilib.model.CourseUnit;
import com.example.unilib.model.Program;

import java.util.ArrayList;
import java.util.List;

/**
 * The programmes and course units the app starts with.
 *
 * <p>The admin can create their own in the admin screen. Until they do, the
 * app falls back to this list so that a student can still register and a
 * lecturer can still upload. The ids here are plain words, not Firestore
 * document ids, so they never clash with the ones the admin creates.
 */
public final class CatalogDefaults {

    public static final String ID_CSC = "csc";
    public static final String ID_IST = "ist";
    public static final String ID_BIT = "bit";

    private CatalogDefaults() { }

    /** The programmes the app ships with. */
    public static List<Program> programs(Context ctx) {
        List<Program> list = new ArrayList<>();
        list.add(new Program(ID_CSC, ctx.getString(R.string.program_computer_science), "CSC"));
        list.add(new Program(ID_IST, ctx.getString(R.string.program_information_technology), "IST"));
        list.add(new Program(ID_BIT, ctx.getString(R.string.program_business_it), "BIT"));
        return list;
    }

    /** The course units that ship with each programme. */
    public static List<CourseUnit> courseUnits(Context ctx) {
        List<CourseUnit> list = new ArrayList<>();
        list.add(new CourseUnit(ID_CSC + "_csc1101", ID_CSC,
                "CSC1101", ctx.getString(R.string.course_programming_fundamentals)));
        list.add(new CourseUnit(ID_CSC + "_csc2101", ID_CSC,
                "CSC2101", ctx.getString(R.string.course_database_systems)));
        list.add(new CourseUnit(ID_CSC + "_csc2202", ID_CSC,
                "CSC2202", ctx.getString(R.string.course_mobile_app_development)));
        list.add(new CourseUnit(ID_IST + "_ist2103", ID_IST,
                "IST2103", ctx.getString(R.string.course_systems_analysis)));
        list.add(new CourseUnit(ID_BIT + "_bit1102", ID_BIT,
                "BIT1102", ctx.getString(R.string.course_computer_networks)));
        return list;
    }

    /** The course units that belong to one programme. */
    public static List<CourseUnit> courseUnits(Context ctx, String programId) {
        List<CourseUnit> all = courseUnits(ctx);
        List<CourseUnit> mine = new ArrayList<>();
        for (CourseUnit unit : all) {
            if (unit.getProgramId() != null && unit.getProgramId().equals(programId)) {
                mine.add(unit);
            }
        }
        return mine;
    }
}
