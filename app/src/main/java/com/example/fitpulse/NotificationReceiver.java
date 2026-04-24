package com.example.fitpulse;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String mealType = intent.getStringExtra("meal_type");
        if (mealType == null) return;

        PreferenceManager pref = new PreferenceManager(context);
        if (!pref.isNotificationsEnabled()) return;

        // Check if meal already logged today
        checkAndShowNotification(context, mealType);
    }

    private void checkAndShowNotification(Context context, String mealType) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("daily_logs").document(today)
                .collection(mealType).get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null && snapshots.isEmpty()) {
                        showNotification(context, mealType);
                    }
                });
    }

    private void showNotification(Context context, String mealType) {
        String title = "Time for " + mealType.substring(0, 1).toUpperCase() + mealType.substring(1) + "!";
        String desc = "What did you eat for " + mealType + "? Tap to log now.";

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "diet_reminder";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Diet Reminders", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("target_fragment", "diet");
        intent.putExtra("open_search", mealType);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pi = PendingIntent.getActivity(context, mealType.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_restaurant)
                .setContentTitle(title)
                .setContentText(desc)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

        nm.notify(mealType.hashCode(), builder.build());
    }
}
