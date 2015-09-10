package me.johannesnz.smsim;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.TextView;

public class Settings extends AppCompatActivity {

    Intent main;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private BroadcastReceiver uiUpdater;
    private CheckedTextView startOnBoot, supressAlerts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        main = new Intent(this, Main.class);
        prefs = getSharedPreferences("SMSIM", MODE_PRIVATE);
        editor = prefs.edit();
        IntentFilter filter = new IntentFilter("me.johannesnz.UPDATE");
        uiUpdater = new UIUpdater();
        registerReceiver(uiUpdater, filter);
        if (prefs.contains("ip")) {
            EditText ipEntry = (EditText) findViewById(R.id.ipAddress);
            ipEntry.setText(prefs.getString("ip", ""), TextView.BufferType.EDITABLE);
        }
        supressAlerts = (CheckedTextView) findViewById(R.id.supressAlerts);
        supressAlerts.setOnClickListener(toggleSupressAlerts);
        supressAlerts.setChecked(prefs.getBoolean("supressAlerts", false));
        startOnBoot = (CheckedTextView) findViewById(R.id.startOnBoot);
        startOnBoot.setOnClickListener(toggleStartOnBoot);
        startOnBoot.setChecked(prefs.getBoolean("startOnBoot", false));
    }

    public void connect(View view) {
        EditText ipEntry = (EditText) findViewById(R.id.ipAddress);
        String ip = ipEntry.getText().toString();
        main.putExtra("ip", ip);
        editor.putString("ip", ip);
        editor.commit();
        startService(main);
    }

    View.OnClickListener toggleSupressAlerts = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            supressAlerts.toggle();
            if (supressAlerts.isChecked()) {
                editor.putBoolean("supressAlerts", true);
            } else {
                editor.putBoolean("supressAlerts", false);
            }
            editor.commit();
        }
    };

    View.OnClickListener toggleStartOnBoot = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            startOnBoot.toggle();
            if (startOnBoot.isChecked()) {
                editor.putBoolean("startOnBoot", true);
            } else {
                editor.putBoolean("startOnBoot", false);
            }
            editor.commit();
        }
    };

    public void exit(View view) {
        stopService(main);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);
        unregisterReceiver(uiUpdater);
        editor.putBoolean("isConnected", false);
        editor.commit();
        finish();
    }

    public class UIUpdater extends BroadcastReceiver {

        public UIUpdater() {

        }

        @Override
        public void onReceive(Context context, Intent intent) {
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(1);
            if (intent.getStringExtra("update").equals("conn") && !prefs.getBoolean("isConnected", false)) {
                editor.putBoolean("isConnected", true);
                editor.commit();

                TextView status = (TextView) findViewById(R.id.status);
                status.setText("Connected");
                status.setTextColor(Color.GREEN);

                TextView address = (TextView) findViewById(R.id.ipAddress);
                address.setEnabled(false);

                Button connect = (Button) findViewById(R.id.connect);
                connect.setEnabled(false);

                Notification not = new Notification(R.mipmap.ic_launcher, "SMSIM: Connected.", System.currentTimeMillis());
                Intent notificationIntent = new Intent(getApplicationContext(), Settings.class);
                PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_NO_CREATE);
                not.flags = Notification.FLAG_ONGOING_EVENT;
                not.setLatestEventInfo(getApplicationContext(), "SMSIM", "Service is running.", pi);
                mNotificationManager.notify(1, not);
            }
            if (intent.getStringExtra("update").equals("disconn") && prefs.getBoolean("isConnected", false)) {
                editor.putBoolean("isConnected", false);
                editor.commit();

                TextView status = (TextView) findViewById(R.id.status);
                status.setText("Disconnected");
                status.setTextColor(Color.RED);

                TextView address = (TextView) findViewById(R.id.ipAddress);
                address.setEnabled(true);

                Button connect = (Button) findViewById(R.id.connect);
                connect.setEnabled(true);

                Notification not = new Notification(R.mipmap.ic_launcher, "SMSIM: Disconnected.", System.currentTimeMillis());
                Intent notificationIntent = new Intent(context, Settings.class);
                PendingIntent pi = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_NO_CREATE);
                not.flags = Notification.FLAG_ONGOING_EVENT;
                not.setLatestEventInfo(context, "SMSIM", "Disconnected.", pi);
                mNotificationManager.notify(1, not);
            }
        }
    }
}
