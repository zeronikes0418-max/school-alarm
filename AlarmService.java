package com.schoolalarm;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class AlarmService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent openApp = new Intent(this, MainActivity.class);
        openApp.putExtra("action", "OPEN_ALARM");
        PendingIntent pi = PendingIntent.getActivity(this, 0, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notif = new NotificationCompat.Builder(this, MainActivity.CHANNEL_ID_HI)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📚 학교알리미")
            .setContentText("알람이 울리고 있어요!")
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build();

        startForeground(1002, notif);
        stopSelf();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
