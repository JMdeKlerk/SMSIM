package me.johannesnz.smsim;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import eneter.messaging.endpoints.stringmessages.DuplexStringMessagesFactory;
import eneter.messaging.endpoints.stringmessages.IDuplexStringMessageSender;
import eneter.messaging.endpoints.stringmessages.IDuplexStringMessagesFactory;
import eneter.messaging.endpoints.stringmessages.StringResponseReceivedEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.net.system.EventHandler;

public class Main extends Service {

    private IDuplexStringMessageSender sender;

    private void openConnection(String ip) throws Exception {
        IDuplexStringMessagesFactory factory = new DuplexStringMessagesFactory();
        sender = factory.createDuplexStringMessageSender();
        sender.responseReceived().subscribe(handler);
        IMessagingSystemFactory messenger = new TcpMessagingSystemFactory();
        IDuplexOutputChannel output = messenger.createDuplexOutputChannel("tcp://" + ip + ":8060/");
        sender.attachDuplexOutputChannel(output);
        int x = 0;
        while (true) {
            x++;
            sender.sendMessage(String.valueOf(x));
            Thread.sleep(5000);
        }
    }

    EventHandler<StringResponseReceivedEventArgs> handler = new EventHandler<StringResponseReceivedEventArgs>() {
        @Override
        public void onEvent(Object o, StringResponseReceivedEventArgs response) {
            Log.i("Log", "Response: " + response.getResponseMessage());
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String ip = intent.getStringExtra("ip");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try { openConnection(ip); } catch (Exception e) { }
            }
        });
        thread.start();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
