package com.example.unilib.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.unilib.model.DocStatus;
import com.example.unilib.model.Document;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Background worker that checks Firestore for newly approved study materials
 * even when the UniLib app is completely closed or in the background.
 */
public class DocCheckWorker extends Worker {

    public DocCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        // 1. Read saved programme info from SharedPreferences
        SharedPreferences userPrefs = context.getSharedPreferences("unilib_user_session", Context.MODE_PRIVATE);
        String programId = userPrefs.getString("program_id", "");
        String programName = userPrefs.getString("program_name", "");

        if (programId.isEmpty() && FirebaseAuth.getInstance().getCurrentUser() != null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            try {
                DocumentSnapshot userSnap = Tasks.await(
                        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                );
                if (userSnap.exists()) {
                    programId = userSnap.getString("programId");
                    programName = userSnap.getString("programName");
                    if (programId != null) userPrefs.edit().putString("program_id", programId).apply();
                    if (programName != null) userPrefs.edit().putString("program_name", programName).apply();
                }
            } catch (ExecutionException | InterruptedException ignored) { }
        }

        if (programId == null || programId.isEmpty()) {
            return Result.success();
        }

        // 2. Query Firestore for approved documents
        try {
            QuerySnapshot snap = Tasks.await(
                    FirebaseFirestore.getInstance().collection("documents")
                            .whereEqualTo("status", DocStatus.APPROVED.name())
                            .get()
            );

            SharedPreferences notifPrefs = context.getSharedPreferences("unilib_notified_docs", Context.MODE_PRIVATE);
            Set<String> notified = new HashSet<>(notifPrefs.getStringSet("notified_ids", new HashSet<>()));
            boolean firstTime = notified.isEmpty();
            Set<String> updatedNotified = new HashSet<>(notified);

            for (DocumentSnapshot docSnap : snap.getDocuments()) {
                Document doc = docSnap.toObject(Document.class);
                if (doc == null || docSnap.getId().isEmpty()) continue;
                doc.setId(docSnap.getId());

                boolean programOk = (dMatch(doc.getProgramId(), programId) || dMatch(doc.getProgramName(), programName));
                if (programOk) {
                    if (!notified.contains(doc.getId())) {
                        if (!firstTime) {
                            NotificationHelper.showApprovedNotification(context, doc);
                        }
                        updatedNotified.add(doc.getId());
                    }
                }
            }
            notifPrefs.edit().putStringSet("notified_ids", updatedNotified).apply();

        } catch (ExecutionException | InterruptedException e) {
            return Result.retry();
        }

        return Result.success();
    }

    private boolean dMatch(String a, String b) {
        return a != null && b != null && !a.trim().isEmpty() && !b.trim().isEmpty() && a.trim().equalsIgnoreCase(b.trim());
    }
}
