package com.example.unilib.data;

import android.content.Context;

import com.example.unilib.R;
import com.example.unilib.model.ActivityLog;
import com.example.unilib.model.DocStatus;
import com.example.unilib.model.Document;
import com.example.unilib.model.Role;
import com.example.unilib.model.User;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Talks to Firestore about documents.
 * Step 8: createDocument. Step 9 will add approve, reject and resubmit methods here.
 */
public class DocumentRepository {

    private final Context ctx;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public DocumentRepository(Context context) {
        this.ctx = context.getApplicationContext();
    }

    private String friendly(Exception e) {
        if (e instanceof FirebaseFirestoreException) {
            switch (((FirebaseFirestoreException) e).getCode()) {
                case PERMISSION_DENIED:
                    return ctx.getString(R.string.err_not_allowed);
                case UNAVAILABLE:
                    return ctx.getString(R.string.err_network);
                default:
                    break;
            }
        }
        return ctx.getString(R.string.err_generic);
    }

    /**
     * Saves a new document record and an "UPLOADED" log entry in one atomic batch.
     * The caller fills title, courseCode, description, fileUrl, filePath, fileSize
     * and (optionally) contributorName. This method fills in the rest.
     *
     * President: status = PENDING.  Lecturer or Admin: status = APPROVED (published at once).
     */
    public void createDocument(Document draft, ResultCallback<Document> callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onError(ctx.getString(R.string.err_session));
            return;
        }
        final String uid = firebaseUser.getUid();

        // Read the user's real role from Firestore (never trust the screen for this)
        db.collection("users").document(uid).get()
                .addOnSuccessListener(snap -> {
                    User user = snap.toObject(User.class);
                    Role role = user == null ? null : user.parsedRole();
                    if (user == null || !user.isActive() || role == null) {
                        callback.onError(ctx.getString(R.string.err_not_allowed));
                        return;
                    }
                    if (role == Role.STUDENT) {
                        callback.onError(ctx.getString(R.string.err_not_allowed));
                        return;
                    }

                    // A lecturer can make a mistake, so their upload waits for a
                    // reviewer. Only an admin publishes straight away.
                    boolean autoApprove = role == Role.ADMIN;

                    draft.setUploaderId(uid);
                    draft.setUploaderName(user.getName());
                    draft.setCreatedAt(Timestamp.now());
                    if (autoApprove) {
                        draft.setStatus(DocStatus.APPROVED);
                        draft.setReviewerId(uid);
                        draft.setReviewedAt(Timestamp.now());
                    } else {
                        draft.setStatus(DocStatus.PENDING);
                    }

                    DocumentReference docRef = db.collection("documents").document();
                    DocumentReference logRef = db.collection("activityLogs").document();

                    ActivityLog log = new ActivityLog();
                    log.setDocumentId(docRef.getId());
                    log.setActorId(uid);
                    log.setActorName(user.getName());
                    log.setAction("UPLOADED");
                    log.setTimestamp(Timestamp.now());

                    WriteBatch batch = db.batch();
                    batch.set(docRef, draft);
                    batch.set(logRef, log);
                    batch.commit()
                            .addOnSuccessListener(v -> {
                                draft.setId(docRef.getId());
                                callback.onSuccess(draft);
                            })
                            .addOnFailureListener(e -> callback.onError(friendly(e)));
                })
                .addOnFailureListener(e -> callback.onError(friendly(e)));
    }

    /**
     * Every document that is still waiting for a reviewer, newest first.
     * Used by the lecturer inbox and by the admin dashboard.
     */
    public void listPending(ResultCallback<List<Document>> callback) {
        db.collection("documents")
                .whereEqualTo("status", DocStatus.PENDING.name())
                .get()
                .addOnSuccessListener(snap -> callback.onSuccess(sortByDate(snap)))
                .addOnFailureListener(e -> callback.onError(friendly(e)));
    }

    /** Everything one member has uploaded, newest first. Used by the president screen. */
    public void listByUploader(String uid, ResultCallback<List<Document>> callback) {
        db.collection("documents")
                .whereEqualTo("uploaderId", uid)
                .get()
                .addOnSuccessListener(snap -> callback.onSuccess(sortByDate(snap)))
                .addOnFailureListener(e -> callback.onError(friendly(e)));
    }

    /**
     * Everything that is in the library, newest first. Used by the student
     * screen, which only shows the documents a reviewer has approved.
     */
    public void listApproved(ResultCallback<List<Document>> callback) {
        db.collection("documents")
                .whereEqualTo("status", DocStatus.APPROVED.name())
                .get()
                .addOnSuccessListener(snap -> callback.onSuccess(sortByDate(snap)))
                .addOnFailureListener(e -> callback.onError(friendly(e)));
    }

    /**
     * Approves or rejects a document. Only a lecturer or an admin may do this,
     * and the role is read from Firestore again rather than trusted from the screen.
     */
    public void review(String documentId, boolean approve, String reason,
                       ResultCallback<Void> callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onError(ctx.getString(R.string.err_session));
            return;
        }
        final String uid = firebaseUser.getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(userSnap -> {
                    User user = userSnap.toObject(User.class);
                    Role role = user == null ? null : user.parsedRole();
                    if (user == null || !user.isActive()
                            || (role != Role.LECTURER && role != Role.ADMIN)) {
                        callback.onError(ctx.getString(R.string.err_not_allowed));
                        return;
                    }

                    DocumentReference docRef = db.collection("documents").document(documentId);
                    DocumentReference logRef = db.collection("activityLogs").document();

                    Map<String, Object> changes = new HashMap<>();
                    changes.put("status", approve ? DocStatus.APPROVED : DocStatus.REJECTED);
                    changes.put("reviewerId", uid);
                    changes.put("reviewedAt", Timestamp.now());
                    if (!approve) changes.put("rejectionReason", reason);

                    ActivityLog log = new ActivityLog();
                    log.setDocumentId(documentId);
                    log.setActorId(uid);
                    log.setActorName(user.getName());
                    log.setAction(approve ? "APPROVED" : "REJECTED");
                    log.setTimestamp(Timestamp.now());

                    // A lecturer cannot check their own work. An admin can, because
                    // the admin is the one who looks over everybody's upload.
                    if (role == Role.LECTURER) {
                        db.collection("documents").document(documentId).get()
                                .addOnSuccessListener(docSnap -> {
                                    String uploaderId = docSnap.getString("uploaderId");
                                    if (uid.equals(uploaderId)) {
                                        callback.onError(ctx.getString(R.string.err_self_review));
                                        return;
                                    }
                                    commitReview(docRef, logRef, changes, log, callback);
                                })
                                .addOnFailureListener(e -> callback.onError(friendly(e)));
                        return;
                    }

                    commitReview(docRef, logRef, changes, log, callback);
                })
                .addOnFailureListener(e -> callback.onError(friendly(e)));
    }

    /** Writes the new status of a document and the log entry in one batch. */
    private void commitReview(DocumentReference docRef, DocumentReference logRef,
                              Map<String, Object> changes, ActivityLog log,
                              ResultCallback<Void> callback) {
        WriteBatch batch = db.batch();
        batch.update(docRef, changes);
        batch.set(logRef, log);
        batch.commit()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(friendly(e)));
    }

    /** Turns a query result into a list, newest document first. */
    private List<Document> sortByDate(QuerySnapshot snap) {
        List<Document> list = new ArrayList<>();
        for (DocumentSnapshot doc : snap.getDocuments()) {
            try {
                Document d = doc.toObject(Document.class);
                if (d != null) {
                    d.setId(doc.getId());
                    list.add(d);
                }
            } catch (RuntimeException e) {
                // Skip a document we cannot read rather than losing the whole list.
            }
        }
        Collections.sort(list, (a, b) -> {
            Timestamp ta = a.getCreatedAt();
            Timestamp tb = b.getCreatedAt();
            if (ta == null) return tb == null ? 0 : 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
        return list;
    }
}