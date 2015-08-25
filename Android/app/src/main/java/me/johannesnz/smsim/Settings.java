package me.johannesnz.smsim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class Settings extends AppCompatActivity {

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private BroadcastReceiver uiUpdater;
    Intent main;

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
    }

    public void connect(View view) {
        EditText ipEntry = (EditText) findViewById(R.id.ipAddress);
        String ip = ipEntry.getText().toString();
        main.putExtra("ip", ip);
        editor.putString("ip", ip);
        editor.commit();
        startService(main);
    }

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
