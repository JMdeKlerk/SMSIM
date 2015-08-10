package com.example.johannes.smsim;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.stringmessages.DuplexStringMessagesFactory;
import eneter.messaging.endpoints.stringmessages.IDuplexStringMessageSender;
import eneter.messaging.endpoints.stringmessages.IDuplexStringMessagesFactory;
import eneter.messaging.endpoints.stringmessages.StringResponseReceivedEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.net.system.EventHandler;

public class MainActivity extends AppCompatActivity {

    public void doMagic(View view) {
        Thread anOpenConnectionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    openConnection();
                } catch (Exception err) {
                    EneterTrace.error("Open connection failed.", err);
                }
            }
        });
        anOpenConnectionThread.start();
    }

    private void openConnection() throws Exception {
        Log.i("Log", "Trying magic...");
        IDuplexStringMessagesFactory factory = new DuplexStringMessagesFactory();
        IDuplexStringMessageSender sender = factory.createDuplexStringMessageSender();
        sender.responseReceived().subscribe(new EventHandler<StringResponseReceivedEventArgs>() {
            @Override
            public void onEvent(Object o, StringResponseReceivedEventArgs stringResponseReceivedEventArgs) {
                Log.i("Log", "Response recieved, ignoring.");
            }
        });
        IMessagingSystemFactory messenger = new TcpMessagingSystemFactory();
        IDuplexOutputChannel output = messenger.createDuplexOutputChannel("tcp://192.168.178.39:8060/");
        sender.attachDuplexOutputChannel(output);
        try {
            sender.sendMessage("Hello world!");
        } catch (Exception err) {
            EneterTrace.error("Sending the message failed.", err);
        }
        Log.i("Log", "Magic should have happened");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
