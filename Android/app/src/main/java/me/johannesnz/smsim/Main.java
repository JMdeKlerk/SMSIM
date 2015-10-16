package me.johannesnz.smsim;

import android.app.Activity;
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

import eneter.messaging.endpoints.stringmessages.IDuplexStringMessageSender;

public class Main extends AppCompatActivity implements OnSharedPreferenceChangeListener {

    public static boolean connected;
    public static long lastPing;
    public static volatile IDuplexStringMessageSender sender;

    private PendingIntent alarmIntent;

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
                    Main.disconnect(activity, "QUIT");
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
        alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        aManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, AlarmManager.INTERVAL_HALF_HOUR, AlarmManager.INTERVAL_HALF_HOUR, alarmIntent);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        if (!prefs.getString("ip", "").equals("")) connect(this);
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

    public static void showNotification(Context context, int id, String message, boolean onGoing) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
        notification.setContentTitle("SMS Messenger");
        notification.setContentText(message);
        if (connected) notification.setSmallIcon(R.mipmap.ic_launcher);
        else notification.setSmallIcon(R.mipmap.ic_notification_bad); // TODO Change to a red icon
        notification.setOngoing(onGoing);
        NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.notify(id, notification.build());
    }

    @Override
    protected void onDestroy() {
        AlarmManager aManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        aManager.cancel(alarmIntent);
        NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.cancel(1);
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
