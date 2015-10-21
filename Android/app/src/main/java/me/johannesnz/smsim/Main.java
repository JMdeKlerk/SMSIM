package me.johannesnz.smsim;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.content.Intent;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.Iterator;
import java.util.List;

import eneter.messaging.endpoints.stringmessages.IDuplexStringMessageSender;

public class Main extends AppCompatActivity implements OnSharedPreferenceChangeListener {

    public static long lastPing;
    public static volatile IDuplexStringMessageSender sender;

    public static class SettingsFragment extends PreferenceFragment implements OnPreferenceClickListener {

        private static Activity activity;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            findPreference("exit").setOnPreferenceClickListener(this);
            findPreference("restart").setOnPreferenceClickListener(this);
            findPreference("donate").setOnPreferenceClickListener(this);
            activity = getActivity();
        }

        @Override
        public boolean onPreferenceClick(Preference pref) {
            switch (pref.getKey()) {
                case ("exit"):
                    activity.finish();
                    break;
                case ("restart"):
                    Main.disconnect(activity, "QUIT");
                    Main.connect(activity);
                    break;
                case ("donate"):
                    // TODO
                    break;
            }
            return true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AlarmManager aManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, PingAlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        aManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, AlarmManager.INTERVAL_HALF_HOUR, AlarmManager.INTERVAL_HALF_HOUR, alarmIntent);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        SharedPreferences.Editor prefsEdit = prefs.edit();
        prefsEdit.putBoolean("shouldContinue", true);
        if (!prefs.getString("ip", "").equals("") && isConnected(this)) connect(this);
        super.onCreate(savedInstanceState);
    }

    public static void connect(Context context) {
        Intent intent = new Intent(context, CommService.class);
        intent.putExtra("command", "connect");
        context.startService(intent);
    }

    public static void sendMessage(Context context, String message) {
        Intent intent = new Intent(context, CommService.class);
        intent.putExtra("command", "send");
        intent.putExtra("data", message);
        context.startService(intent);
    }

    public static void disconnect(Context context, String status) {
        Intent intent = new Intent(context, CommService.class);
        intent.putExtra("command", "disconnect");
        intent.putExtra("data", status);
        context.startService(intent);
    }

    public static boolean isConnected(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("connected", false);
    }

    public static void setConnected(Context context, boolean state) {
        SharedPreferences.Editor prefsEdit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        prefsEdit.putBoolean("connected", state);
        prefsEdit.commit();
    }

    public static void showNotification(Context context, int id, String message, boolean onGoing) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
        notification.setContentTitle("SMS Messenger");
        notification.setContentText(message);
        if (isConnected(context)) notification.setSmallIcon(R.mipmap.ic_launcher);
        else notification.setSmallIcon(R.mipmap.ic_notification_bad);
        notification.setOngoing(onGoing);
        NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.notify(id, notification.build());
    }

    @Override
    protected void onDestroy() {
        disconnect(this, "QUIT");
        SharedPreferences.Editor prefsEdit = PreferenceManager.getDefaultSharedPreferences(this).edit();
        prefsEdit.putBoolean("connected", false);
        prefsEdit.putBoolean("retryInProgress", false);
        prefsEdit.putBoolean("shouldContinue", false);
        prefsEdit.commit();
        NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.cancelAll();
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals("ip")) {
            disconnect(this, "QUIT");
            connect(this);
        }
    }

}
