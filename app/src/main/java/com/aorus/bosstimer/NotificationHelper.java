package com.aorus.bosstimer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationHelper {

/*
 * Novo canal para evitar que o Android mantenha
 * configurações antigas do canal anterior.
 */
public static final String CHANNEL = "boss_alerts_v2";

public static void createChannel(Context c) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        NotificationManager manager =
                c.getSystemService(
                        NotificationManager.class
                );

        /*
         * Som padrão do Android
         */
        Uri soundUri =
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;

        AudioAttributes audioAttributes =
                new AudioAttributes.Builder()
                        .setUsage(
                                AudioAttributes.USAGE_NOTIFICATION
                        )
                        .setContentType(
                                AudioAttributes.CONTENT_TYPE_SONIFICATION
                        )
                        .build();

        NotificationChannel ch =
                new NotificationChannel(
                        CHANNEL,
                        "Alertas de Boss",
                        NotificationManager.IMPORTANCE_HIGH
                );

        ch.setDescription(
                "Avisos de nascimento dos bosses"
        );

        /*
         * Vibração
         */
        ch.enableVibration(true);

        ch.setVibrationPattern(
                new long[]{
                        0,
                        250,
                        150,
                        400,
                        150,
                        600
                }
        );

        /*
         * Som
         */
        ch.setSound(
                soundUri,
                audioAttributes
        );

        /*
         * Mostrar conteúdo na tela bloqueada
         */
        ch.setLockscreenVisibility(
                NotificationCompat.VISIBILITY_PUBLIC
        );

        manager.createNotificationChannel(ch);
    }
}

public static void notify(
        Context c,
        int id,
        String title,
        String text
) {

    createChannel(c);

    /*
     * Ao tocar na notificação, abre o aplicativo.
     */
    Intent launch =
            c.getPackageManager()
                    .getLaunchIntentForPackage(
                            c.getPackageName()
                    );

    PendingIntent pi = null;

    if (launch != null) {

        pi = PendingIntent.getActivity(
                c,
                id,
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );
    }

    NotificationCompat.Builder builder =
            new NotificationCompat.Builder(
                    c,
                    CHANNEL
            );

    builder.setSmallIcon(
            android.R.drawable.ic_lock_idle_alarm
    );

    builder.setContentTitle(title);

    builder.setContentText(text);

    builder.setStyle(
            new NotificationCompat.BigTextStyle()
                    .bigText(text)
    );

    /*
     * Alta prioridade para aparecer como alerta.
     */
    builder.setPriority(
            NotificationCompat.PRIORITY_HIGH
    );

    builder.setCategory(
            NotificationCompat.CATEGORY_ALARM
    );

    /*
     * Permite mostrar o conteúdo na tela bloqueada.
     */
    builder.setVisibility(
            NotificationCompat.VISIBILITY_PUBLIC
    );

    builder.setAutoCancel(true);

    /*
     * Vibração.
     */
    builder.setVibrate(
            new long[]{
                    0,
                    250,
                    150,
                    400,
                    150,
                    600
            }
    );

    /*
     * Som padrão.
     * O som principal é definido pelo NotificationChannel.
     */
    builder.setSound(
            android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
    );

    if (pi != null) {

        builder.setContentIntent(pi);
    }

    NotificationManager manager =
            c.getSystemService(
                    NotificationManager.class
            );

    manager.notify(
            id,
            builder.build()
    );
}


}
