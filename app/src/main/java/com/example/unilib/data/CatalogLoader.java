package com.example.unilib.data;

import android.content.Context;

import com.example.unilib.model.CourseUnit;
import com.example.unilib.model.Program;

import java.util.List;

/**
 * Gives the screens the list of programmes and course units to show.
 *
 * <p>It reads the catalogue the admin maintains in Firestore. When Firestore has
 * nothing yet, or when the read is not allowed, it falls back to the list the
 * app ships with, so a student can always register and a lecturer can always
 * upload.
 *
 * <p>Programmes and course units always come from the same place. When the
 * fallback is in use the course units of the built in programmes are used too,
 * so the two never fall out of step with each other.
 */
public class CatalogLoader {

    private final Context ctx;
    private final CatalogRepository repo;
    /** True once the built in list is being used instead of Firestore. */
    private boolean usingDefaults = false;

    public CatalogLoader(Context context) {
        this.ctx = context.getApplicationContext();
        this.repo = new CatalogRepository(this.ctx);
    }

    /** The programmes to show in the dropdown. */
    public void loadPrograms(ResultCallback<List<Program>> callback) {
        repo.listPrograms(new ResultCallback<List<Program>>() {
            @Override public void onSuccess(List<Program> programs) {
                if (programs == null || programs.isEmpty()) {
                    usingDefaults = true;
                    callback.onSuccess(CatalogDefaults.programs(ctx));
                } else {
                    usingDefaults = false;
                    callback.onSuccess(programs);
                }
            }

            @Override public void onError(String message) {
                // Not allowed to read, or offline. The built in list still works.
                usingDefaults = true;
                callback.onSuccess(CatalogDefaults.programs(ctx));
            }
        });
    }

    /** The course units of one programme, to show in the dropdown under it. */
    public void loadCourseUnits(String programId, ResultCallback<List<CourseUnit>> callback) {
        if (usingDefaults) {
            callback.onSuccess(CatalogDefaults.courseUnits(ctx, programId));
            return;
        }
        repo.listCourseUnits(programId, callback);
    }
}
