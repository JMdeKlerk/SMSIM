package me.johannesnz.smsim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.TextView;

public class Settings extends AppCompatActivity {

    Intent main;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private BroadcastReceiver uiUpdater;
    private CheckedTextView startOnBoot, autoReconnect;

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
        autoReconnect = (CheckedTextView) findViewById(R.id.autoReconnect);
        autoReconnect.setOnClickListener(toggleAutoReconnect);
        startOnBoot = (CheckedTextView) findViewById(R.id.startOnBoot);
        startOnBoot.setOnClickListener(toggleStartOnBoot);
    }

    public void connect(View view) {
        EditText ipEntry = (EditText) findViewById(R.id.ipAddress);
        String ip = ipEntry.getText().toString();
        main.putExtra("ip", ip);
        editor.putString("ip", ip);
        editor.commit();
        startService(main);
    }

    View.OnClickListener toggleAutoReconnect = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            autoReconnect.toggle();
            if (autoReconnect.isChecked()) {
                editor.putBoolean("autoReconnect", true);
            }
            else {
                editor.putBoolean("autoReconnect", false);
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
            }
            else {
                editor.putBoolean("startOnBoot", false);
            }
            editor.commit();
        }
    };

    public void exit(View view) {
        stopService(main);
        unregisterReceiver(uiUpdater);
        finish();
    }

    public class UIUpdater extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra("update").equals("conn")) {
                TextView status = (TextView) findViewById(R.id.status);
                status.setText("Connected");
            }
        }

        public UIUpdater() {

        }
    }
}
