package me.johannesnz.smsim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WakeupReceiver extends BroadcastReceiver {

    public WakeupReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if ((System.currentTimeMillis() - Main.lastPing) > 60000) {
            Main.getInstance().connFail();
        }
    }

}
