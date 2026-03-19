package com.schoolalarm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // 부팅 완료 시 알람 복원
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            "android.intent.action.MY_PACKAGE_REPLACED".equals(action)) {
            // SharedPreferences에서 알람 시간 읽어 재등록
            restoreAlarm(context);
            return;
        }

        // 알람 발동
        String label = intent.getStringExtra("label");
        if (label == null) label = "학교알리미 알람";

        // 화면 깨우기
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
            "SchoolAlarm::AlarmWakeLock");
        wl.acquire(10 * 1000L);

        // 앱 열기 인텐트
        Intent openApp = new Intent(context, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openApp.putExtra("action", "OPEN_ALARM");
        PendingIntent pi = PendingIntent.getActivity(context, 0, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 알림 생성
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID_HI)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📚 학교알리미 ⏰")
            .setContentText(label)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText("🎒 알람이 울렸어요!\n탭하여 오늘 준비물을 확인하세요."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setVibrate(new long[]{0, 600, 200, 600, 200, 600, 200, 1000})
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setOngoing(false)
            .setFullScreenIntent(pi, true); // 잠금화면에서도 표시

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(1001, builder.build());

        // 앱이 백그라운드면 바로 실행
        openApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(openApp);

        wl.release();
    }

    private void restoreAlarm(Context context) {
        // SharedPreferences에서 저장된 알람 시간 복원
        android.content.SharedPreferences prefs =
            context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE);
        boolean alarmOn = prefs.getBoolean("alarm_on", false);
        if (!alarmOn) return;

        int hour = prefs.getInt("alarm_hour", 7);
        int minute = prefs.getInt("alarm_minute", 0);

        android.app.AlarmManager am = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("com.schoolalarm.ALARM_TRIGGER");
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, hour);
        cal.set(java.util.Calendar.MINUTE, minute);
        cal.set(java.util.Calendar.SECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        } else {
            am.setExact(android.app.AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        }
    }
}
