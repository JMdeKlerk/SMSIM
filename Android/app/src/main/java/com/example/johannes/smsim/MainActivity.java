package com.example.johannes.smsim;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.typedmessages.DuplexTypedMessagesFactory;
import eneter.messaging.endpoints.typedmessages.IDuplexTypedMessageSender;
import eneter.messaging.endpoints.typedmessages.IDuplexTypedMessagesFactory;
import eneter.messaging.endpoints.typedmessages.TypedResponseReceivedEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.net.system.EventHandler;

public class MainActivity extends AppCompatActivity {

    public static class MyRequest {
        public String Text;
    }

    public static class MyResponse {
        public int Length;
    }

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
        IDuplexTypedMessagesFactory aSenderFactory = new DuplexTypedMessagesFactory();
        IDuplexTypedMessageSender<MyResponse, MyRequest> mySender = aSenderFactory.createDuplexTypedMessageSender(MyResponse.class, MyRequest.class);
        mySender.responseReceived().subscribe(new EventHandler<TypedResponseReceivedEventArgs<MyResponse>>() {
            @Override
            public void onEvent(Object o, TypedResponseReceivedEventArgs<MyResponse> myResponseTypedResponseReceivedEventArgs) {
                Log.i("Log", "Response recieved, ignoring.");
            }
        });
        IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
        IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://192.168.178.39:8060/");
        mySender.attachDuplexOutputChannel(anOutputChannel);
        final MyRequest aRequestMsg = new MyRequest();
        aRequestMsg.Text = "Hello world!";
        try {
            mySender.sendRequestMessage(aRequestMsg);
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
