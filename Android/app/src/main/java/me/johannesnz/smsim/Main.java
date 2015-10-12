package me.johannesnz.smsim;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.IntentFilter;
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
    public static IDuplexStringMessageSender sender;
    public static WakeupReceiver lastPingWakeupCheck;

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
                    Main.disconnect(activity);
                    activity.finish();
                    break;
                case ("restart"):
                    Main.disconnect(activity);
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
        lastPingWakeupCheck = new WakeupReceiver();
        registerReceiver(lastPingWakeupCheck, new IntentFilter((Intent.ACTION_SCREEN_ON)));
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

    public static void disconnect(Context context) {
        Intent intent = new Intent(context, CommService.class);
        intent.putExtra("command", "disconnect");
        context.startService(intent);
    }

    public static void showNotification(Context context, int id, String message, boolean onGoing) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
        notification.setContentTitle("SMS Messenger");
        notification.setContentText(message);
        if (connected) notification.setSmallIcon(R.mipmap.ic_launcher);
        else notification.setSmallIcon(R.mipmap.ic_launcher); // TODO Change to a red icon
        notification.setOngoing(onGoing);
        // TODO Tap to restart
        NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.notify(id, notification.build());
    }

    @Override
    protected void onDestroy() {
        NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.cancel(1);
        sender.detachDuplexOutputChannel();
        unregisterReceiver(lastPingWakeupCheck);
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals("ip")) {
            disconnect(this);
            connect(this);
        }
    }

}
