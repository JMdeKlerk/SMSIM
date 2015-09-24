package me.johannesnz.smsim;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.preference.PreferenceManager;
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

    public static Main main;

    private SharedPreferences prefs;
    private IDuplexStringMessageSender sender;
    private BroadcastReceiver messageReceiver;

    public Main() {
        main = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showNotification("Service starting...", false);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        messageReceiver = new SMSReceiver(this);
        IntentFilter smsFilter = new IntentFilter();
        smsFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        smsFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        registerReceiver(messageReceiver, smsFilter);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String ip = prefs.getString("ip", "");
                final IDuplexStringMessagesFactory factory = new DuplexStringMessagesFactory();
                final IMessagingSystemFactory messenger = new TcpMessagingSystemFactory();
                sender = factory.createDuplexStringMessageSender();
                try {
                    IDuplexOutputChannel output = messenger.createDuplexOutputChannel("tcp://" + ip + ":8060/");
                    sender.attachDuplexOutputChannel(output);
                    sender.responseReceived().subscribe(new EventHandler<StringResponseReceivedEventArgs>() {
                        @Override
                        public void onEvent(Object o, StringResponseReceivedEventArgs response) {
                            handleResponse(response.getResponseMessage());
                        }
                    });
                    sendMessage("Conn:" + Build.MODEL);
                    showNotification("Service is running.", true);
                } catch (Exception ex) {
                    showNotification("Connection failed. Tap to retry.", false);
                    stopSelf();
                }
            }
        }).start();
        return START_STICKY;
    }

    public void sendMessage(String message) {
        try {
            sender.sendMessage(message);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleResponse(String message) {
        if (message.startsWith("Ack:Conn")) {
            Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            while (phones.moveToNext()) {
                int phoneType = phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                if (phoneType == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) {
                    String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String pic = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                    sendMessage("Contact:" + name + ":" + phoneNumber + ":" + pic);
                }
            }
            phones.close();
        }
        if (message.startsWith("SMS:")) {
            String[] input = message.split(":");
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(input[1], null, input[2], null, null);
        }
        if (message.equals("DC")) {
            showNotification("Connection failed. Tap to retry.", false);
            stopSelf();
        }
    }

    private void showNotification(String message, boolean persistent) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
        notification.setSmallIcon(R.mipmap.ic_launcher);
        notification.setContentTitle("SMS Messenger");
        notification.setContentText(message);
        notification.setOngoing(persistent);
        if (!persistent) {
            Intent restart = new Intent(this, Main.class);
            PendingIntent pi = PendingIntent.getService(this, 0, restart, 0);
            notification.setContentIntent(pi);
            notification.setAutoCancel(true);
        }
        NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.cancel(1);
        nManager.notify(1, notification.build());
    }

    public void cleanQuit() {
        NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        sendMessage("DC");
        nManager.cancel(1);
        unregisterReceiver(messageReceiver);
        sender.detachDuplexOutputChannel();
        stopSelf();
        System.exit(0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sendMessage("DC");
        unregisterReceiver(messageReceiver);
        sender.detachDuplexOutputChannel();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
