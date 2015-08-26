package me.johannesnz.smsim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootStarter extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            SharedPreferences prefs = context.getSharedPreferences("SMSIM", context.MODE_PRIVATE);
            if (prefs.contains("ip") && prefs.getBoolean("startOnBoot", false)) {
                String ip = prefs.getString("ip", "");
                Intent main = new Intent(context, Main.class);
                main.putExtra("ip", ip);
                context.startService(main);
            }
        }
    }

}
