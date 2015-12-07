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
import android.util.Log;

import com.android.vending.util.IabHelper;
import com.android.vending.util.IabResult;
import com.android.vending.util.Purchase;

import eneter.messaging.endpoints.stringmessages.IDuplexStringMessageSender;

public class Main extends AppCompatActivity implements OnSharedPreferenceChangeListener {

    public static long lastPing;
    public static volatile IDuplexStringMessageSender sender;

    public static class SettingsFragment extends PreferenceFragment implements OnPreferenceClickListener {

        private static Activity activity;
        private IabHelper billingHelper;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            findPreference("exit").setOnPreferenceClickListener(this);
            findPreference("restart").setOnPreferenceClickListener(this);
            findPreference("donate").setOnPreferenceClickListener(this);
            findPreference("share").setOnPreferenceClickListener(this);
            activity = getActivity();
            String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyesaLxbMV4dFvMQ" +
                    "s3uefeEwri0NBJG6+OPKVVQNwkOHL9l/yyBDHYEZhwM8SZXqIIbsfniM33itgTZad/Xguz+kcxpJ76txGiS" +
                    "DX/iK5i0C6y6vat6NiEb0EvHdvveew+gbbixUNnbCbKNBXNJoxYjD+ySZzDS+o4aomCwKcxsnjaefivuRh1" +
                    "pWAupRGPF1sfu5N2htTr6c8LvHDVg00OIhMY8DcD/wVo5TYhkikOJXzw35hLvoQVplj4zQP2PnE7jYngUms" +
                    "zSwfCkQD3bb8wunvju0JhwrNTqDTVB84W90AgxW5xv6I3ieafYbc1tLznlBvaocCze01+fli+721OQIDAQAB";
            billingHelper = new IabHelper(activity, base64EncodedPublicKey);
            billingHelper.startSetup(null);
        }

        IabHelper.OnIabPurchaseFinishedListener donationFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
            @Override
            public void onIabPurchaseFinished(IabResult result, Purchase info) {
                if (result.isSuccess()) Log.i("Log", info.getSku() + " purchased");
            }
        };

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
                    try {
                        billingHelper.launchPurchaseFlow(activity, "donate_1", 0, donationFinishedListener);
                    } catch (IllegalStateException e) {
                    }
                    break;
                case ("share"):
                    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");
                    String shareBody = "A free app to turn your SMS messages into IMs - get it here: https://play.google.com/store/apps/details?id=me.johannesnz.smsim";
                    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "IM-ify your texts");
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                    startActivity(Intent.createChooser(sharingIntent, "Share via:"));
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
        aManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, AlarmManager.INTERVAL_FIFTEEN_MINUTES, AlarmManager.INTERVAL_FIFTEEN_MINUTES, alarmIntent);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        if (!prefs.getString("ip", "").equals("") && !isConnected(this)) connect(this);
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean("hideNotifications", false)) {
            NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
            notification.setContentTitle("SMS Messenger");
            notification.setContentText(message);
            if (isConnected(context)) notification.setSmallIcon(R.mipmap.ic_launcher);
            else notification.setSmallIcon(R.mipmap.ic_notification_bad);
            notification.setOngoing(onGoing);
            NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nManager.notify(id, notification.build());
        }
    }

    @Override
    protected void onDestroy() {
        disconnect(this, "QUIT");
        SharedPreferences.Editor prefsEdit = PreferenceManager.getDefaultSharedPreferences(this).edit();
        prefsEdit.putBoolean("connected", false);
        prefsEdit.putBoolean("retryInProgress", false);
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
        if (key.equals("hideNotifications")) {
            NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nManager.cancelAll();
        }
    }

}
