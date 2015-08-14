package me.johannesnz.smsim;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.telephony.SmsMessage;
import android.util.Log;

import eneter.messaging.endpoints.stringmessages.DuplexStringMessagesFactory;
import eneter.messaging.endpoints.stringmessages.IDuplexStringMessageSender;
import eneter.messaging.endpoints.stringmessages.IDuplexStringMessagesFactory;
import eneter.messaging.endpoints.stringmessages.StringResponseReceivedEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.net.system.EventHandler;

public class Main extends Service{

    private IDuplexStringMessageSender sender;
    private BroadcastReceiver messageReceiver;

    @Override
    public void onCreate() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        messageReceiver = new Receiver();
        registerReceiver(messageReceiver, filter);
    }

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
        return START_REDELIVER_INTENT;
    }

    public class Receiver extends BroadcastReceiver  {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
                Bundle bundle = intent.getExtras();
                SmsMessage[] messages;
                if (bundle != null) {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    messages = new SmsMessage[pdus.length];
                    for (int i = 0; i < messages.length; i++) {
                        messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        String from = messages[i].getOriginatingAddress();
                        String body = messages[i].getMessageBody();
                        try { sender.sendMessage("SMS:" + from + ":" + body); } catch (Exception e) { }
                    }
                }
            }
        }
        public Receiver() {}
    }

    private void openConnection(String ip) throws Exception {
        IDuplexStringMessagesFactory factory = new DuplexStringMessagesFactory();
        sender = factory.createDuplexStringMessageSender();
        sender.responseReceived().subscribe(handler);
        IMessagingSystemFactory messenger = new TcpMessagingSystemFactory();
        IDuplexOutputChannel output = messenger.createDuplexOutputChannel("tcp://" + ip + "/");
        sender.attachDuplexOutputChannel(output);
        sender.sendMessage("Conn:" + Build.MODEL);
    }

    EventHandler<StringResponseReceivedEventArgs> handler = new EventHandler<StringResponseReceivedEventArgs>() {
        @Override
        public void onEvent(Object o, StringResponseReceivedEventArgs response) {
            if (response.getResponseMessage().startsWith("Ack: Conn")) {
                Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                while (phones.moveToNext()) {
                    String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    try { sender.sendMessage("Contact:" + name + ":" + phoneNumber); } catch (Exception e) { }
                }
                phones.close();
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
