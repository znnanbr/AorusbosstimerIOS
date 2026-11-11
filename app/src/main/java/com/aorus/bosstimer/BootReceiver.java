package com.aorus.bosstimer;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c, Intent i){ if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction()) && c.getSharedPreferences("boss_timer",0).getBoolean("enabled",false)) BossScheduler.scheduleAll(c); }
}
