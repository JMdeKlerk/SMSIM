package me.johannesnz.smsim;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class Settings extends AppCompatActivity {

    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = getSharedPreferences("SMSIM", MODE_PRIVATE);
        editor = prefs.edit();
        if (prefs.contains("ip")) {
            EditText ipEntry = (EditText) findViewById(R.id.ipAddress);
            ipEntry.setText(prefs.getString("ip", ""), TextView.BufferType.EDITABLE);
        }
    }

    public void connect(View view) {
        EditText ipEntry = (EditText) findViewById(R.id.ipAddress);
        String ip = ipEntry.getText().toString();
        Intent main = new Intent(this, Main.class);
        main.putExtra("ip", ip);
        editor.putString("ip", ip);
        editor.commit();
        startService(main);
    }
}
