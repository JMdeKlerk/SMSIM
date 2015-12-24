package me.johannesnz.smsim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PingAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("Log", "Checking ping");
        if (Main.isConnected(context) && (System.currentTimeMillis() - Main.lastPing) > 1000 * 60 * 15) {
            Main.disconnect(context, "PING TIMEOUT");
        }
    }

}
