package me.johannesnz.smsim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class WifiReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected() || (activeNetworkInfo.isConnected() && activeNetworkInfo.getType() != ConnectivityManager.TYPE_WIFI)) {
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("me.johannesnz.UPDATE");
            broadcastIntent.putExtra("update", "disconn");
            context.sendBroadcast(broadcastIntent);
        }
    }
}
