package com.schoolalarm;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    public static final String CHANNEL_ID = "school_alarm_channel";
    public static final String CHANNEL_ID_HI = "school_alarm_hi";

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;

    private final ActivityResultLauncher<String[]> permLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            // 권한 결과 처리 — WebView에 전달
            if (webView != null) webView.evaluateJavascript("window.onPermissionResult && window.onPermissionResult()", null);
        });

    private final ActivityResultLauncher<Intent> filePickerLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (filePathCallback != null) {
                Uri[] uris = WebChromeClient.FileChooserParams.parseResult(result.getResultCode(), result.getData());
                filePathCallback.onReceiveValue(uris);
                filePathCallback = null;
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 상태바 색상
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFF0F2D20);
        }

        setContentView(R.layout.activity_main);
        createNotificationChannels();
        requestEssentialPermissions();

        webView = findViewById(R.id.webview);
        setupWebView();

        // intent로 알람 화면 열기 처리
        handleAlarmIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleAlarmIntent(intent);
    }

    private void handleAlarmIntent(Intent intent) {
        if (intent != null && "OPEN_ALARM".equals(intent.getStringExtra("action"))) {
            if (webView != null) {
                webView.postDelayed(() ->
                    webView.evaluateJavascript("window.openAlarmFromNative && window.openAlarmFromNative()", null), 800);
            }
        }
    }

    private void setupWebView() {
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setSupportZoom(false);
        ws.setBuiltInZoomControls(false);
        ws.setDisplayZoomControls(false);
        // 삼성 인터넷과 같은 렌더링 엔진 사용
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ws.setSafeBrowsingEnabled(false);
        }

        // JavaScript → Android 브릿지
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                    WebChromeClient.FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;
                Intent intent = fileChooserParams.createIntent();
                try {
                    filePickerLauncher.launch(intent);
                } catch (Exception e) {
                    MainActivity.this.filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        // assets/index.html 로드
        webView.loadUrl("file:///android_asset/index.html");
    }

    // ─── Android Bridge ───────────────────────────────
    public class AndroidBridge {

        /** 알람 등록 (정확한 시스템 알람) */
        @JavascriptInterface
        public void setAlarm(String timeJson) {
            try {
                JSONObject obj = new JSONObject(timeJson);
                int hour   = obj.getInt("hour");
                int minute = obj.getInt("minute");
                String repeat = obj.optString("repeat", "daily");
                String label   = obj.optString("label", "학교알리미 알람");

                AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(MainActivity.this, AlarmReceiver.class);
                intent.setAction("com.schoolalarm.ALARM_TRIGGER");
                intent.putExtra("label", label);

                PendingIntent pi = PendingIntent.getBroadcast(
                    MainActivity.this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, minute);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (am.canScheduleExactAlarms()) {
                        am.setAlarmClock(new AlarmManager.AlarmClockInfo(cal.getTimeInMillis(), pi), pi);
                    } else {
                        am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                }

                showToastOnUI(hour + ":" + String.format("%02d", minute) + " 알람 설정됨 ✅");
            } catch (Exception e) {
                showToastOnUI("알람 설정 실패: " + e.getMessage());
            }
        }

        /** 알람 취소 */
        @JavascriptInterface
        public void cancelAlarm() {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(MainActivity.this, AlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(
                MainActivity.this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.cancel(pi);
            showToastOnUI("알람이 취소됐어요");
        }

        /** 진동 */
        @JavascriptInterface
        public void vibrate(String pattern) {
            // JS vibrate API가 이미 동작하므로 추가 처리 없음
        }

        /** 알림 권한 요청 */
        @JavascriptInterface
        public void requestNotificationPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread(() -> permLauncher.launch(new String[]{Manifest.permission.POST_NOTIFICATIONS}));
                }
            }
        }

        /** 알림 권한 상태 반환 */
        @JavascriptInterface
        public String getNotificationPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    ? "granted" : "denied";
            }
            return "granted"; // Android 12 이하는 자동 허용
        }

        /** 앱 버전 정보 */
        @JavascriptInterface
        public String getAppInfo() {
            return "{\"platform\":\"android\",\"version\":\"1.0.0\",\"sdk\":" + Build.VERSION.SDK_INT + "}";
        }

        private void showToastOnUI(String msg) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
        }
    }

    // ─── 알림 채널 생성 ──────────────────────────────
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);

            // 일반 알람 채널
            NotificationChannel ch1 = new NotificationChannel(
                CHANNEL_ID, "학교알리미 알람",
                NotificationManager.IMPORTANCE_HIGH);
            ch1.setDescription("매일 아침 준비물 알림");
            ch1.enableVibration(true);
            ch1.setVibrationPattern(new long[]{0, 600, 200, 600, 200, 600});
            ch1.enableLights(true);
            nm.createNotificationChannel(ch1);

            // 긴급 채널 (알람 소리와 함께)
            NotificationChannel ch2 = new NotificationChannel(
                CHANNEL_ID_HI, "학교알리미 긴급알람",
                NotificationManager.IMPORTANCE_MAX);
            ch2.setDescription("알람 시간 알림");
            ch2.enableVibration(true);
            ch2.setVibrationPattern(new long[]{0, 600, 200, 600, 200, 600, 200, 1000});
            ch2.enableLights(true);
            ch2.setBypassDnd(true);
            nm.createNotificationChannel(ch2);
        }
    }

    // ─── 필수 권한 요청 ──────────────────────────────
    private void requestEssentialPermissions() {
        java.util.List<String> perms = new java.util.ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!perms.isEmpty()) {
            permLauncher.launch(perms.toArray(new String[0]));
        }

        // 배터리 최적화 제외 요청 (삼성 절전 모드 대응)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
