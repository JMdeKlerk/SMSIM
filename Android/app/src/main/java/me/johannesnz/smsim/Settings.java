package me.johannesnz.smsim;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

public class Settings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    public void connect(View view) {
        EditText ipEntry = (EditText) findViewById(R.id.ipAddress);
        String ip = ipEntry.getText().toString();
        Intent main = new Intent(this, Main.class);
        main.putExtra("ip", ip);
        startService(main);
    }
}
