package com.example.unilib.data;

import android.content.Context;

import com.example.unilib.R;
import com.example.unilib.model.CourseUnit;
import com.example.unilib.model.Program;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Reads and writes the catalogue of programmes and course units.
 *
 * <p>Two Firestore collections hold it:
 * <ul>
 *   <li>{@code programs} - the programmes an admin has created.</li>
 *   <li>{@code courseUnits} - the course units, each pointing at the programme
 *       it belongs to through its {@code programId} field.</li>
 * </ul>
 */
public class CatalogRepository {

    private final Context ctx;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public CatalogRepository(Context context) {
        this.ctx = context.getApplicationContext();
    }

    private String friendly(Exception e) {
        if (e instanceof FirebaseFirestoreException) {
            switch (((FirebaseFirestoreException) e).getCode()) {
                case PERMISSION_DENIED:
                    // The catalogue needs its own rules, so this is the message
                    // that points at them.
                    return ctx.getString(R.string.err_catalog_rules);
                case UNAVAILABLE:
                    return ctx.getString(R.string.err_network);
                default:
                    break;
            }
        }
        return ctx.getString(R.string.err_generic);
    }

    /** Every programme, in the order the admin put them in. */
    public void listPrograms(ResultCallback<List<Program>> callback) {
        db.collection("programs").get()
                .addOnSuccessListener(snap -> callback.onSuccess(readPrograms(snap)))
                .addOnFailureListener(e -> callback.onError(friendly(e)));
    }

    /** The course units that belong to one programme. */
    public void listCourseUnits(String programId, ResultCallback<List<CourseUnit>> callback) {
        if (programId == null || programId.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        db.collection("courseUnits")
                .whereEqualTo("programId", programId)
                .get()
                .addOnSuccessListener(snap -> callback.onSuccess(readCourseUnits(snap)))
                .addOnFailureListener(e -> callback.onError(friendly(e)));
    }

    /** Adds a programme. */
    public void createProgram(String name, String code, ResultCallback<Program> callback) {
        Program program = new Program(null, name, code);
        program.setActive(true);
        program.setCreatedAt(Timestamp.now());
        DocumentReference ref = db.collection("programs").document();
        ref.set(program)
                .addOnSuccessListener(v -> {
                    program.setId(ref.getId());
                    callback.onSuccess(program);
                })
                .addOnFailureListener(e -> callback.onError(friendly(e)));
    }

    /** Adds a course unit under a programme. */
    public void createCourseUnit(String programId, String code, String title,
                                 ResultCallback<CourseUnit> callback) {
        if (programId == null || programId.isEmpty()) {
            callback.onError(ctx.getString(R.string.err_no_program));
            return;
        }
        CourseUnit unit = new CourseUnit(null, programId, code, title);
        unit.setActive(true);
        unit.setCreatedAt(Timestamp.now());
        DocumentReference ref = db.collection("courseUnits").document();
        ref.set(unit)
                .addOnSuccessListener(v -> {
                    unit.setId(ref.getId());
                    callback.onSuccess(unit);
                })
                .addOnFailureListener(e -> callback.onError(friendly(e)));
    }

    /**
     * Copies the programmes and course units the app ships with into Firestore,
     * so the admin can then rename, add to or remove them. Anything that is
     * already there is left alone.
     */
    public void seedDefaults(ResultCallback<Integer> callback) {
        listPrograms(new ResultCallback<List<Program>>() {
            @Override public void onSuccess(List<Program> existing) {
                if (existing != null && !existing.isEmpty()) {
                    callback.onSuccess(0);
                    return;
                }
                seedPrograms(new ResultCallback<List<Program>>() {
                    @Override public void onSuccess(List<Program> created) {
                        seedCourseUnits(created, callback);
                    }
                    @Override public void onError(String message) {
                        callback.onError(message);
                    }
                });
            }
            @Override public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private void seedPrograms(ResultCallback<List<Program>> callback) {
        List<Program> defaults = CatalogDefaults.programs(ctx);
        List<Program> created = new ArrayList<>();
        for (Program program : defaults) {
            Program copy = new Program(null, program.getName(), program.getCode());
            copy.setActive(true);
            copy.setCreatedAt(Timestamp.now());
            DocumentReference ref = db.collection("programs").document();
            ref.set(copy)
                    .addOnSuccessListener(v -> {
                        copy.setId(ref.getId());
                        created.add(copy);
                        if (created.size() == defaults.size()) callback.onSuccess(created);
                    })
                    .addOnFailureListener(e -> callback.onError(friendly(e)));
        }
    }

    private void seedCourseUnits(List<Program> programs, ResultCallback<Integer> callback) {
        List<CourseUnit> defaults = CatalogDefaults.courseUnits(ctx);
        List<CourseUnit> wanted = new ArrayList<>();
        for (CourseUnit unit : defaults) {
            Program owner = null;
            for (Program program : programs) {
                if (program.getCode() != null && program.getCode().equals(programPrefix(unit))) {
                    owner = program;
                    break;
                }
            }
            if (owner != null) {
                CourseUnit copy = new CourseUnit(null, owner.getId(), unit.getCode(), unit.getTitle());
                copy.setActive(true);
                copy.setCreatedAt(Timestamp.now());
                wanted.add(copy);
            }
        }
        if (wanted.isEmpty()) {
            callback.onSuccess(0);
            return;
        }
        int[] done = {0};
        for (CourseUnit copy : wanted) {
            DocumentReference ref = db.collection("courseUnits").document();
            ref.set(copy)
                    .addOnSuccessListener(v -> {
                        done[0]++;
                        if (done[0] == wanted.size()) callback.onSuccess(done[0]);
                    })
                    .addOnFailureListener(e -> callback.onError(friendly(e)));
        }
    }

    /** The three letters a course number starts with, for example "CSC". */
    private String programPrefix(CourseUnit unit) {
        String code = unit.getCode();
        if (code == null) return "";
        int end = 0;
        while (end < code.length() && Character.isLetter(code.charAt(end))) end++;
        return code.substring(0, end);
    }

    private List<Program> readPrograms(QuerySnapshot snap) {
        List<Program> list = new ArrayList<>();
        for (DocumentSnapshot doc : snap.getDocuments()) {
            try {
                Program program = doc.toObject(Program.class);
                if (program != null) {
                    program.setId(doc.getId());
                    list.add(program);
                }
            } catch (RuntimeException e) {
                // Skip a programme we cannot read rather than losing the list.
            }
        }
        Collections.sort(list, new Comparator<Program>() {
            @Override public int compare(Program a, Program b) {
                long at = a.getCreatedAt() == null ? 0 : a.getCreatedAt().getSeconds();
                long bt = b.getCreatedAt() == null ? 0 : b.getCreatedAt().getSeconds();
                if (at != bt) return Long.compare(at, bt);
                String an = a.getName() == null ? "" : a.getName();
                String bn = b.getName() == null ? "" : b.getName();
                return an.compareToIgnoreCase(bn);
            }
        });
        return list;
    }

    private List<CourseUnit> readCourseUnits(QuerySnapshot snap) {
        List<CourseUnit> list = new ArrayList<>();
        for (DocumentSnapshot doc : snap.getDocuments()) {
            try {
                CourseUnit unit = doc.toObject(CourseUnit.class);
                if (unit != null) {
                    unit.setId(doc.getId());
                    list.add(unit);
                }
            } catch (RuntimeException e) {
                // Skip a course unit we cannot read rather than losing the list.
            }
        }
        Collections.sort(list, new Comparator<CourseUnit>() {
            @Override public int compare(CourseUnit a, CourseUnit b) {
                String ac = a.getCode() == null ? "" : a.getCode();
                String bc = b.getCode() == null ? "" : b.getCode();
                return ac.compareToIgnoreCase(bc);
            }
        });
        return list;
    }
}
