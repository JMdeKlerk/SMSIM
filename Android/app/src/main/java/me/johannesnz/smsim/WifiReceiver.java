package me.johannesnz.smsim;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
        if (!activeNetworkInfo.isConnected() || (activeNetworkInfo.isConnected() && activeNetworkInfo.getType() != ConnectivityManager.TYPE_WIFI)) {
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("me.johannesnz.UPDATE");
            broadcastIntent.putExtra("update", "disconn");
            context.sendBroadcast(broadcastIntent);
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(1);
            Notification not = new Notification(R.mipmap.ic_launcher, "SMSIM: Disconnected.", System.currentTimeMillis());
            Intent notificationIntent = new Intent(context, Settings.class);
            PendingIntent pi = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_NO_CREATE);
            not.flags = Notification.FLAG_ONGOING_EVENT;
            not.setLatestEventInfo(context, "SMSIM", "Disconnected.", pi);
            mNotificationManager.notify(1, not);
        }
    }
}
