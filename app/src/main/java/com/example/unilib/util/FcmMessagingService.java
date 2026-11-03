package com.example.unilib.util;

import androidx.annotation.NonNull;

import com.example.unilib.model.Document;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Handles incoming Firebase Cloud Messaging (FCM) push notifications
 * when the app is closed or running in the background.
 */
public class FcmMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();
        if (data.containsKey("doc_title") || data.containsKey("title")) {
            Document doc = new Document();
            doc.setId(data.get("doc_id"));
            doc.setTitle(data.get("doc_title"));
            doc.setProgramName(data.get("program_name"));
            NotificationHelper.showApprovedNotification(getApplicationContext(), doc);
        } else if (remoteMessage.getNotification() != null) {
            Document doc = new Document();
            doc.setTitle(remoteMessage.getNotification().getTitle());
            doc.setProgramName(remoteMessage.getNotification().getBody());
            NotificationHelper.showApprovedNotification(getApplicationContext(), doc);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }
}
