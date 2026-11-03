package com.example.unilib.data;

import android.content.Context;

import com.example.unilib.model.CourseUnit;
import com.example.unilib.model.Program;

import java.util.ArrayList;
import java.util.List;

/**
 * Gives the screens the list of programmes and course units from Firestore.
 * Only the programmes and course units created by the Admin in Firestore are displayed.
 */
public class CatalogLoader {

    private final Context ctx;
    private final CatalogRepository repo;

    public CatalogLoader(Context context) {
        this.ctx = context.getApplicationContext();
        this.repo = new CatalogRepository(this.ctx);
    }

    /** Loads only the programmes created by the Admin in Firestore. */
    public void loadPrograms(ResultCallback<List<Program>> callback) {
        repo.listPrograms(new ResultCallback<List<Program>>() {
            @Override public void onSuccess(List<Program> programs) {
                callback.onSuccess(programs == null ? new ArrayList<>() : programs);
            }

            @Override public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /** Loads only the course units created by the Admin in Firestore under a programme. */
    public void loadCourseUnits(String programId, ResultCallback<List<CourseUnit>> callback) {
        repo.listCourseUnits(programId, new ResultCallback<List<CourseUnit>>() {
            @Override public void onSuccess(List<CourseUnit> units) {
                callback.onSuccess(units == null ? new ArrayList<>() : units);
            }

            @Override public void onError(String message) {
                callback.onError(message);
            }
        });
    }
}
