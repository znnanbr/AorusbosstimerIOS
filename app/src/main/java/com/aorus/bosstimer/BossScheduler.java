package com.aorus.bosstimer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.TimeZone;

public class BossScheduler {

    static final String ACTION =
            "com.aorus.bosstimer.BOSS_ALARM";

    static final TimeZone BRT =
            TimeZone.getTimeZone("America/Sao_Paulo");

    /*
     * Tipos:
     *
     * 0 = 5 minutos
     * 1 = 1 minuto
     * 2 = nascimento
     */
    static final int[] OFFSETS = {
            300,
            60,
            0
    };

    public static void scheduleAll(Context c) {

        SharedPreferences p =
                c.getSharedPreferences(
                        "boss_timer",
                        Context.MODE_PRIVATE
                );

        if (!p.getBoolean("enabled", false)) {
            return;
        }

        String json =
                p.getString("bosses", "[]");

        try {

            JSONArray a =
                    new JSONArray(json);

            for (int i = 0; i < a.length(); i++) {

                JSONObject boss =
                        a.getJSONObject(i);

                for (int type = 0; type < 3; type++) {

                    if (isTypeEnabled(c, type)) {
                        scheduleOne(c, boss, type);
                    }
                }
            }

        } catch (Exception ignored) {
        }
    }

    static boolean isTypeEnabled(
            Context c,
            int type
    ) {

        SharedPreferences p =
                c.getSharedPreferences(
                        "boss_timer",
                        Context.MODE_PRIVATE
                );

        String json =
                p.getString(
                        "alert_types",
                        "[\"5\",\"1\",\"spawn\"]"
                );

        try {

            JSONArray a =
                    new JSONArray(json);

            String wanted;

            if (type == 0) {
                wanted = "5";
            } else if (type == 1) {
                wanted = "1";
            } else {
                wanted = "spawn";
            }

            for (int i = 0; i < a.length(); i++) {

                if (wanted.equals(
                        a.getString(i)
                )) {
                    return true;
                }
            }

        } catch (Exception ignored) {
        }

        return false;
    }

    static void scheduleOne(
            Context c,
            JSONObject boss,
            int type
    ) {

        try {

            long spawn =
                    findNextSpawn(boss);

            if (spawn < 0) {
                return;
            }

            long when =
                    spawn -
                    OFFSETS[type] * 1000L;

            /*
             * Não agenda algo que já passou.
             */
            if (when <=
                    System.currentTimeMillis() + 1500) {
                return;
            }

            int id =
                    (
                            boss.optString("id")
                            + "-"
                            + type
                    )
                    .hashCode()
                    & 0x7fffffff;

            Intent in =
                    new Intent(
                            c,
                            BossAlarmReceiver.class
                    );

            in.setAction(ACTION);

            in.putExtra(
                    "boss",
                    boss.toString()
            );

            in.putExtra(
                    "type",
                    type
            );

            in.putExtra(
                    "id",
                    id
            );

            PendingIntent pi =
                    PendingIntent.getBroadcast(
                            c,
                            id,
                            in,
                            PendingIntent.FLAG_UPDATE_CURRENT
                                    | PendingIntent.FLAG_IMMUTABLE
                    );

            AlarmManager am =
                    (AlarmManager)
                            c.getSystemService(
                                    Context.ALARM_SERVICE
                            );

            if (am == null) {
                return;
            }

            /*
             * Android 12+ exige permissão para
             * alarmes exatos.
             */
            if (Build.VERSION.SDK_INT >= 31) {

                if (!am.canScheduleExactAlarms()) {
                    return;
                }
            }

            /*
             * ALARM_CLOCK:
             *
             * É mais apropriado para um evento que
             * precisa acordar o aparelho.
             *
             * Diferente do setExactAndAllowWhileIdle(),
             * o Android trata esse alarme como um
             * alarme visível/importante para o usuário.
             */
            if (Build.VERSION.SDK_INT >= 21) {

                Intent showIntent =
                        c.getPackageManager()
                                .getLaunchIntentForPackage(
                                        c.getPackageName()
                                );

                PendingIntent showPendingIntent =
                        null;

                if (showIntent != null) {

                    showPendingIntent =
                            PendingIntent.getActivity(
                                    c,
                                    id + 100000,
                                    showIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                                            | PendingIntent.FLAG_IMMUTABLE
                            );
                }

                AlarmManager.AlarmClockInfo alarmInfo =
                        new AlarmManager.AlarmClockInfo(
                                when,
                                showPendingIntent
                        );

                am.setAlarmClock(
                        alarmInfo,
                        pi
                );

            } else {

                am.setExact(
                        AlarmManager.RTC_WAKEUP,
                        when,
                        pi
                );
            }

        } catch (Exception ignored) {
        }
    }

    static long findNextSpawn(
            JSONObject b
    ) throws Exception {

        Calendar now =
                Calendar.getInstance(BRT);

        now.set(
                Calendar.MILLISECOND,
                0
        );

        int today =
                now.get(Calendar.DAY_OF_WEEK) - 1;

        for (int d = 0; d < 8; d++) {

            int day =
                    (today + d) % 7;

            JSONArray times =
                    getTimesForDay(
                            b,
                            day
                    );

            for (
                    int i = 0;
                    i < times.length();
                    i++
            ) {

                String[] hm =
                        times
                                .getString(i)
                                .split(":");

                Calendar x =
                        (Calendar) now.clone();

                x.add(
                        Calendar.DAY_OF_YEAR,
                        d
                );

                x.set(
                        Calendar.HOUR_OF_DAY,
                        Integer.parseInt(hm[0])
                );

                x.set(
                        Calendar.MINUTE,
                        Integer.parseInt(hm[1])
                );

                x.set(
                        Calendar.SECOND,
                        0
                );

                x.set(
                        Calendar.MILLISECOND,
                        0
                );

                if (
                        x.getTimeInMillis()
                        >
                        System.currentTimeMillis()
                        + 1000
                ) {

                    return x.getTimeInMillis();
                }
            }
        }

        return -1;
    }

    static JSONArray getTimesForDay(
            JSONObject b,
            int day
    ) throws Exception {

        if (
                "daily".equals(
                        b.optString(
                                "scheduleType"
                        )
                )
        ) {

            return b.optJSONArray("times")
                    == null
                    ? new JSONArray()
                    : b.getJSONArray("times");
        }

        JSONArray days =
                b.optJSONArray("days");

        boolean ok = false;

        for (
                int i = 0;
                i <
                (
                        days == null
                                ? 0
                                : days.length()
                );
                i++
        ) {

            if (
                    days.getInt(i)
                    == day
            ) {

                ok = true;
                break;
            }
        }

        if (!ok) {
            return new JSONArray();
        }

        JSONObject map =
                b.optJSONObject(
                        "dayTimeMap"
                );

        if (map != null) {

            return map.optJSONArray(
                    String.valueOf(day)
            ) == null
                    ? new JSONArray()
                    : map.getJSONArray(
                            String.valueOf(day)
                    );
        }

        return b.optJSONArray("times")
                == null
                ? new JSONArray()
                : b.getJSONArray("times");
    }

    public static void cancelAll(
            Context c
    ) {

        SharedPreferences p =
                c.getSharedPreferences(
                        "boss_timer",
                        Context.MODE_PRIVATE
                );

        String json =
                p.getString(
                        "bosses",
                        "[]"
                );

        try {

            JSONArray a =
                    new JSONArray(json);

            AlarmManager am =
                    (AlarmManager)
                            c.getSystemService(
                                    Context.ALARM_SERVICE
                            );

            if (am == null) {
                return;
            }

            for (
                    int i = 0;
                    i < a.length();
                    i++
            ) {

                for (
                        int type = 0;
                        type < 3;
                        type++
                ) {

                    int id =
                            (
                                    a.getJSONObject(i)
                                            .optString("id")
                                    + "-"
                                    + type
                            )
                            .hashCode()
                            & 0x7fffffff;

                    Intent in =
                            new Intent(
                                    c,
                                    BossAlarmReceiver.class
                            );

                    in.setAction(ACTION);

                    PendingIntent pi =
                            PendingIntent.getBroadcast(
                                    c,
                                    id,
                                    in,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                                            | PendingIntent.FLAG_IMMUTABLE
                            );

                    am.cancel(pi);
                }
            }

        } catch (Exception ignored) {
        }
    }
}
