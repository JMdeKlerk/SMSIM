package me.johannesnz.smsim;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.InputStream;

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
    public static boolean connected;
    public static long lastPing;

    private static Thread mainThread;
    private WakeupReceiver lastPingWakeupCheck;
    private static IDuplexStringMessageSender sender;
    private SharedPreferences prefs;

    public Main() {
        main = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        lastPingWakeupCheck = new WakeupReceiver(this);
        registerReceiver(lastPingWakeupCheck, new IntentFilter((Intent.ACTION_SCREEN_ON)));
        mainThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (!setUp()) connFail(true);
            }
        });
        mainThread.start();
        return START_STICKY;
    }

    private boolean setUp() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
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
            connected = true;
            sendMessage("Version");
            showNotification(1, "Connected.", true);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public void sendMessage(String message) {
        try {
            sender.sendMessage(message);
        } catch (Exception e) {
            connFail(true);
        }
    }

    private void handleResponse(String message) {
        lastPing = System.currentTimeMillis();
        Log.i("Log", message);
        if (message.substring(2).equals("Ping")) {
            sendMessage("Pong");
        }
        if (message.substring(2).startsWith("Version:")) {
            String version = message.split(":")[2];
            if (version.equals("1.0")) sendMessage("Conn:" + Build.MODEL);
            else connFail(false);
        }
        if (message.substring(2).equals("Req:Contacts")) {
            Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            while (phones.moveToNext()) {
                int phoneType = phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                if (phoneType == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) {
                    String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String contactID = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                    InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, new Long(contactID)));
                    if (inputStream != null) {
                        Bitmap photo = BitmapFactory.decodeStream(inputStream);
                        sendMessage("Contact:" + name + ":" + phoneNumber + ":" + photo.toString());
                    } else {
                        sendMessage("Contact:" + name + ":" + phoneNumber);
                    }
                }
            }
            phones.close();
        }
        if (message.substring(2).startsWith("SMS:")) {
            String[] input = message.split(":");
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(input[2], null, input[3], null, null);
            sendMessage("Success:" + input[0]);
        }
        if (message.substring(2).equals("DC")) {
            connFail(true);
        }
    }

    private void showNotification(int id, String message, boolean canCancel) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
        if (connected) notification.setSmallIcon(R.mipmap.ic_launcher);
        else notification.setSmallIcon(R.mipmap.ic_launcher); // TODO Change to a red icon
        notification.setContentTitle("SMS Messenger");
        notification.setContentText(message);
        notification.setOngoing(!canCancel);
        Intent restart = new Intent(this, Main.class);
        PendingIntent pi = PendingIntent.getService(this, 0, restart, 0);
        notification.setContentIntent(pi);
        notification.setAutoCancel(true);
        NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.notify(id, notification.build());
    }

    public void connFail(Boolean canRetry) {
        connected = false;
        if (canRetry) {
            while (!Thread.interrupted() && prefs.getBoolean("autoRetry", false) && !setUp()) {
                showNotification(1, "Connection failed. Auto-retrying.", true);
                android.os.SystemClock.sleep(Integer.parseInt(prefs.getString("autoRetryInterval", "300")) * 1000);
            }
            if (!prefs.getBoolean("autoRetry", false))
                showNotification(1, "Connection failed. Tap to retry.", false);
        } else {
            showNotification(2, "Your PC client is out of date. Please download the latest version.", true);
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.cancel(1);
        if (connected) new Thread(new Runnable() {
            @Override
            public void run() {
                sendMessage("DC");
            }
        }).start();
        sender.detachDuplexOutputChannel();
        unregisterReceiver(lastPingWakeupCheck);
        mainThread.interrupt();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
