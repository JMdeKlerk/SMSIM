package me.johannesnz.smsim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WakeupReceiver extends BroadcastReceiver {

    private Main main;

    public WakeupReceiver(Main parent) {
        main = parent;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (main.connected && (System.currentTimeMillis() - Main.lastPing) > 60000) {
            main.connFail(true);
        }
    }

}
