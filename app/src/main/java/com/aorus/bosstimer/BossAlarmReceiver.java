package com.aorus.bosstimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONObject;

public class BossAlarmReceiver extends BroadcastReceiver {


@Override
public void onReceive(Context c, Intent in) {

    try {

        JSONObject boss = new JSONObject(
                in.getStringExtra("boss")
        );

        int type = in.getIntExtra("type", 0);
        int id = in.getIntExtra("id", 1);

        String name = boss.optString(
                "name",
                "Boss"
        );

        String title;
        String text;

        if (type == 0) {

            title = "🔴 BOSS EM 5 MINUTOS";

            text = "O BOSS "
                    + name.toUpperCase()
                    + " NASCERÁ EM 5 MINUTOS";

        } else if (type == 1) {

            title = "🚨 BOSS EM 1 MINUTO";

            text = "O BOSS "
                    + name.toUpperCase()
                    + " NASCERÁ EM 1 MINUTO";

        } else {

            title = "⚔️ BOSS NASCEU!";

            text = "O BOSS "
                    + name.toUpperCase()
                    + " NASCEU";
        }

        NotificationHelper.notify(
                c,
                id,
                title,
                text
        );

        Intent speechIntent = new Intent(
                c,
                BossSpeechService.class
        );

        speechIntent.putExtra(
                "text",
                text
        );

        if (android.os.Build.VERSION.SDK_INT >= 26) {

            c.startForegroundService(
                    speechIntent
            );

        } else {

            c.startService(
                    speechIntent
            );
        }

        BossScheduler.scheduleOne(
                c,
                boss,
                type
        );

    } catch (Exception e) {

        e.printStackTrace();
    }
}


}
