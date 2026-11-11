package com.aorus.bosstimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;

import androidx.core.app.NotificationCompat;

import java.util.Locale;

public class BossSpeechService extends Service {


private TextToSpeech tts;

private static final String CHANNEL_ID =
        "boss_speech_service";

private static final int NOTIFICATION_ID =
        9999;

@Override
public void onCreate() {

    super.onCreate();

    createServiceChannel();

    Notification notification =
            new NotificationCompat.Builder(
                    this,
                    CHANNEL_ID
            )
            .setSmallIcon(
                    android.R.drawable.ic_lock_idle_alarm
            )
            .setContentTitle(
                    "Boss Timer"
            )
            .setContentText(
                    "Alerta de Boss"
            )
            .setPriority(
                    NotificationCompat.PRIORITY_LOW
            )
            .setOngoing(true)
            .build();

    startForeground(
            NOTIFICATION_ID,
            notification
    );
}

@Override
public int onStartCommand(
        Intent intent,
        int flags,
        int startId
) {

    if (intent == null) {

        stopSpeech();
        return START_NOT_STICKY;
    }

    String text =
            intent.getStringExtra("text");

    if (text == null || text.isEmpty()) {

        stopSpeech();
        return START_NOT_STICKY;
    }

    speak(text);

    return START_NOT_STICKY;
}

private void speak(String text) {

    if (tts != null) {

        tts.stop();
        tts.shutdown();
        tts = null;
    }

    tts = new TextToSpeech(
            getApplicationContext(),
            status -> {

                if (status != TextToSpeech.SUCCESS) {

                    stopSpeech();
                    return;
                }

                int result =
                        tts.setLanguage(
                                new Locale(
                                        "pt",
                                        "BR"
                                )
                        );

                if (result ==
                        TextToSpeech.LANG_MISSING_DATA
                        ||
                        result ==
                        TextToSpeech.LANG_NOT_SUPPORTED) {

                    stopSpeech();
                    return;
                }

                tts.setSpeechRate(1.0f);
                tts.setPitch(1.0f);

                tts.setOnUtteranceProgressListener(
                        new android.speech.tts.UtteranceProgressListener() {

                            @Override
                            public void onStart(
                                    String utteranceId
                            ) {
                            }

                            @Override
                            public void onDone(
                                    String utteranceId
                            ) {

                                stopSpeech();
                            }

                            @Override
                            public void onError(
                                    String utteranceId
                            ) {

                                stopSpeech();
                            }
                        }
                );

                tts.speak(
                        text,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "BOSS_ALERT"
                );
            }
    );
}

private void stopSpeech() {

    if (tts != null) {

        tts.stop();
        tts.shutdown();
        tts = null;
    }

    if (Build.VERSION.SDK_INT >= 24) {
        stopForeground(STOP_FOREGROUND_REMOVE);
    } else {
        stopForeground(true);
    }

    stopSelf();
}

private void createServiceChannel() {

    if (Build.VERSION.SDK_INT >= 26) {

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "Serviço de voz do Boss Timer",
                        NotificationManager.IMPORTANCE_LOW
                );

        channel.setDescription(
                "Serviço utilizado para falar os alertas dos bosses"
        );

        NotificationManager manager =
                getSystemService(
                        NotificationManager.class
                );

        manager.createNotificationChannel(channel);
    }
}

@Override
public IBinder onBind(Intent intent) {

    return null;
}

@Override
public void onDestroy() {

    if (tts != null) {

        tts.stop();
        tts.shutdown();
        tts = null;
    }

    super.onDestroy();
}


}
