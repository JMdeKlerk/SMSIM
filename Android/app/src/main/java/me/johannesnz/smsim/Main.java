package me.johannesnz.smsim;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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

    private SharedPreferences prefs;
    private IDuplexStringMessageSender sender;
    private BroadcastReceiver messageReceiver;

    @Override
    public void onCreate() {
        prefs = getSharedPreferences("SMSIM", MODE_PRIVATE);
        messageReceiver = new SMSReceiver(this, sender);
        IntentFilter smsFilter = new IntentFilter();
        smsFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        smsFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        registerReceiver(messageReceiver, smsFilter);
    }

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
                            Thread.sleep(300000);
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
    }

    public void updateConnectionStatus(String status) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("me.johannesnz.UPDATE");
        broadcastIntent.putExtra("update", status);
        sendBroadcast(broadcastIntent);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);
        if (status.equals("conn")) {
            Notification not = new Notification(R.mipmap.ic_launcher, "SMSIM: Connected.", System.currentTimeMillis());
            Intent notificationIntent = new Intent(getApplicationContext(), Settings.class);
            PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_NO_CREATE);
            not.flags = Notification.FLAG_ONGOING_EVENT;
            not.setLatestEventInfo(getApplicationContext(), "SMSIM", "Service is running.", pi);
            mNotificationManager.notify(1, not);
        }
        if (status.equals("disconn")) {
            Notification not = new Notification(R.mipmap.ic_launcher, "SMSIM: Disconnected.", System.currentTimeMillis());
            Intent notificationIntent = new Intent(getApplicationContext(), Settings.class);
            PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_NO_CREATE);
            not.flags = Notification.FLAG_ONGOING_EVENT;
            not.setLatestEventInfo(getApplicationContext(), "SMSIM", "Disconnected.", pi);
            mNotificationManager.notify(1, not);
        }
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
            e.printStackTrace();
        }
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);
        unregisterReceiver(messageReceiver);
        sender.detachDuplexOutputChannel();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
