package com.aorus.bosstimer;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.*;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private static final int REQ_NOTIF = 1001;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);

        webView.setBackgroundColor(0xFF0D0A07);
        webView.setWebViewClient(new WebViewClient());

        webView.addJavascriptInterface(
                new AndroidBridge(this),
                "AndroidNotifications"
        );

        webView.loadUrl("file:///android_asset/index.html");

        NotificationHelper.createChannel(this);

        if (getIntent().getBooleanExtra("open_notifications", false)) {
            webView.postDelayed(
                    () -> requestNotificationPermission(),
                    500
            );
        }
    }

    public void requestNotificationPermission() {

        if (android.os.Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQ_NOTIF
            );
        }

        if (android.os.Build.VERSION.SDK_INT >= 31) {

            AlarmManager am =
                    (AlarmManager) getSystemService(ALARM_SERVICE);

            if (am != null && !am.canScheduleExactAlarms()) {

                try {
                    startActivity(
                            new Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:" + getPackageName())
                            )
                    );
                } catch (Exception ignored) {}
            }
        }
    }

    public static class AndroidBridge {

        private final MainActivity activity;

        AndroidBridge(MainActivity a) {
            activity = a;
        }

        @JavascriptInterface
        public void requestPermission() {
            activity.runOnUiThread(
                    activity::requestNotificationPermission
            );
        }

        /*
         * Ativa as notificações Android.
         */
        @JavascriptInterface
        public void enableNotifications(String bossesJson) {

            activity.getSharedPreferences(
                    "boss_timer",
                    MODE_PRIVATE
            ).edit()
                    .putString("bosses", bossesJson)
                    .putBoolean("enabled", true)
                    .apply();

            activity.runOnUiThread(() -> {

                activity.requestNotificationPermission();

                BossScheduler.scheduleAll(activity);

                Toast.makeText(
                        activity,
                        "Notificações ativadas",
                        Toast.LENGTH_SHORT
                ).show();
            });
        }

        /*
         * Desativa TODAS as notificações.
         */
        @JavascriptInterface
        public void disableNotifications() {

            activity.getSharedPreferences(
                    "boss_timer",
                    MODE_PRIVATE
            ).edit()
                    .putBoolean("enabled", false)
                    .apply();

            BossScheduler.cancelAll(activity);

            activity.runOnUiThread(() ->
                    Toast.makeText(
                            activity,
                            "Notificações desativadas",
                            Toast.LENGTH_SHORT
                    ).show()
            );
        }

        /*
         * Atualiza quais tipos de alerta estão selecionados.
         *
         * 5 = alerta de 5 minutos
         * 1 = alerta de 1 minuto
         * 0 = alerta de nascimento
         */
        @JavascriptInterface
        public void setAlertTypes(String typesJson) {

            activity.getSharedPreferences(
                    "boss_timer",
                    MODE_PRIVATE
            ).edit()
                    .putString("alert_types", typesJson)
                    .apply();

            /*
             * Reagenda tudo de acordo com as novas escolhas.
             */
            BossScheduler.cancelAll(activity);

            boolean enabled = activity
                    .getSharedPreferences(
                            "boss_timer",
                            MODE_PRIVATE
                    )
                    .getBoolean("enabled", false);

            if (enabled) {
                BossScheduler.scheduleAll(activity);
            }
        }

        /*
         * Mantém compatibilidade com a V2.
         */
        @JavascriptInterface
        public void schedule(String bossesJson) {

            enableNotifications(bossesJson);
        }

        /*
         * Mantém compatibilidade com código antigo.
         */
        @JavascriptInterface
        public void cancel() {

            disableNotifications();
        }
    }
}
