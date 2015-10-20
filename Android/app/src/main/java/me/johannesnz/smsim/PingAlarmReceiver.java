package me.johannesnz.smsim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PingAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Main.isConnected(context) && (System.currentTimeMillis() - Main.lastPing) > 1000 * 60 * 30) {
            Main.disconnect(context, "PING TIMEOUT");
        }
    }

}
