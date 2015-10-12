package me.johannesnz.smsim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WakeupReceiver extends BroadcastReceiver {

    public WakeupReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Main.connected && (System.currentTimeMillis() - CommService.lastPing) > 60000) {
            Main.disconnect(context);
        }
    }

}
