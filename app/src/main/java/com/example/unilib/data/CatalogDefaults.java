package com.example.unilib.data;

import android.content.Context;

import com.example.unilib.model.CourseUnit;
import com.example.unilib.model.Program;

import java.util.ArrayList;
import java.util.List;

/**
 * Empty default catalogue provider.
 * All programmes and course units are managed dynamically by the Admin in Firestore.
 */
public final class CatalogDefaults {

    private CatalogDefaults() { }

    public static List<Program> programs(Context ctx) {
        return new ArrayList<>();
    }

    public static List<CourseUnit> courseUnits(Context ctx) {
        return new ArrayList<>();
    }

    public static List<CourseUnit> courseUnits(Context ctx, String programId) {
        return new ArrayList<>();
    }
}
