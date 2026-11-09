package com.example.unilib.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.unilib.R;
import com.example.unilib.model.Document;
import com.example.unilib.ui.auth.LoginActivity;
import com.example.unilib.ui.common.PdfViewerActivity;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Posts system notifications when a new document is approved for the student's programme.
 * If user session is active, tapping takes user straight to PdfViewerActivity.
 * If user session is expired, tapping takes user to LoginActivity.
 */
public class NotificationHelper {

    private static final String CHANNEL_ID = "unilib_approved_docs";
    private static final String CHANNEL_NAME = "Approved Study Materials";
    private static final String CHANNEL_DESC = "Notifications when new study materials are approved for your programme.";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void showApprovedNotification(Context context, Document document) {
        if (document == null) return;

        createNotificationChannel(context);

        Intent intent;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // Active session: open document straight in PdfViewerActivity
            intent = new Intent(context, PdfViewerActivity.class);
            intent.putExtra(PdfViewerActivity.EXTRA_DOC_ID, document.getId());
            intent.putExtra(PdfViewerActivity.EXTRA_DOC_TITLE, document.getTitle());
            intent.putExtra(PdfViewerActivity.EXTRA_DOC_URL, document.getFileUrl());
            intent.putExtra(PdfViewerActivity.EXTRA_DOC_PROGRAM, document.getProgramName());
            intent.putExtra(PdfViewerActivity.EXTRA_DOC_COURSE, document.getCourseCode());
        } else {
            // Expired/signed out: open LoginActivity
            intent = new Intent(context, LoginActivity.class);
            intent.putExtra(PdfViewerActivity.EXTRA_DOC_ID, document.getId());
            intent.putExtra(PdfViewerActivity.EXTRA_DOC_TITLE, document.getTitle());
            intent.putExtra(PdfViewerActivity.EXTRA_DOC_URL, document.getFileUrl());
            intent.putExtra(PdfViewerActivity.EXTRA_DOC_PROGRAM, document.getProgramName());
            intent.putExtra(PdfViewerActivity.EXTRA_DOC_COURSE, document.getCourseCode());
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                document.getId() == null ? 0 : document.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = context.getString(R.string.notif_doc_approved_title);
        String program = document.getProgramName() == null ? "" : document.getProgramName();
        String message = context.getString(R.string.notif_doc_approved_msg,
                document.getTitle() == null ? "Study Document" : document.getTitle(),
                program);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pdf)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            int id = document.getId() == null ? (int) System.currentTimeMillis() : document.getId().hashCode();
            manager.notify(id, builder.build());
        } catch (SecurityException ignored) {
            // Permission not granted on Android 13+
        }
    }
}
