package me.johannesnz.smsim;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Build;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
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

    public IDuplexStringMessageSender sender;
    private BroadcastReceiver messageReceiver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String ip = intent.getStringExtra("ip");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    openConnection(ip);
                    while (true) {
                        try {
                            sender.sendMessage("Ping");
                            Thread.sleep(90000);
                        } catch (Exception e) {
                            updateConnectionStatus("disconn");
                            openConnection(ip);
                        }
                    }
                } catch (Exception e) {
                    Log.e("Log", e.toString());
                }
            }
        }

        );
        thread.start();
        return START_REDELIVER_INTENT;
    }

    private void openConnection(String ip) throws Exception {
        IDuplexStringMessagesFactory factory = new DuplexStringMessagesFactory();
        sender = factory.createDuplexStringMessageSender();
        sender.responseReceived().subscribe(handler);
        IMessagingSystemFactory messenger = new TcpMessagingSystemFactory();
        IDuplexOutputChannel output = messenger.createDuplexOutputChannel("tcp://" + ip + ":8060/");
        sender.attachDuplexOutputChannel(output);
        sender.sendMessage("Conn:" + Build.MODEL);
        messageReceiver = new SMSReceiver(this);
        IntentFilter smsFilter = new IntentFilter();
        smsFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        smsFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        registerReceiver(messageReceiver, smsFilter);
    }

    public void updateConnectionStatus(String status) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("me.johannesnz.UPDATE");
        broadcastIntent.putExtra("update", status);
        sendBroadcast(broadcastIntent);
    }

    EventHandler<StringResponseReceivedEventArgs> handler = new EventHandler<StringResponseReceivedEventArgs>() {
        @Override
        public void onEvent(Object o, StringResponseReceivedEventArgs response) {
            if (response.getResponseMessage().startsWith("Ack: Conn")) {
                updateConnectionStatus("conn");
                Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                while (phones.moveToNext()) {
                    int phoneType = phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                    if (phoneType == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) {
                        String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        String pic = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                        try {
                            sender.sendMessage("Contact:" + name + ":" + phoneNumber + ":" + pic);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                phones.close();
            }
            if (response.getResponseMessage().startsWith("SMS:")) {
                String[] input = response.getResponseMessage().split(":");
                SmsManager sms = SmsManager.getDefault();
                sms.sendTextMessage(input[1], null, input[2], null, null);
            }
        }
    };

    @Override
    public void onDestroy() {
        try {
            sender.sendMessage("DC");
        } catch (Exception e) {

        }
        unregisterReceiver(messageReceiver);
        sender.detachDuplexOutputChannel();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
