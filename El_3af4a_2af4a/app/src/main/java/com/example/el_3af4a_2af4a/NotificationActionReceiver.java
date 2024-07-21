package com.example.el_3af4a_2af4a;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

public class NotificationActionReceiver extends BroadcastReceiver {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String ID = intent.getStringExtra("ID");
        int notificationId = intent.getIntExtra("notification", -1);

        if ("YES_ACTION".equals(action)) {
            updateFeedback(ID, 1);
        } else if ("NO_ACTION".equals(action)) {
            updateFeedback(ID, -1);
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationId != -1) {
            notificationManager.cancel(notificationId);
        }

        Toast.makeText(context, "Thanks for your feedback :)", Toast.LENGTH_SHORT).show();
    }

    private void updateFeedback(String obstacleId, int feedbackValue) {
        db.collection("Coordinates").document(obstacleId)
                .update("feedback", com.google.firebase.firestore.FieldValue.increment(feedbackValue));
    }
}
