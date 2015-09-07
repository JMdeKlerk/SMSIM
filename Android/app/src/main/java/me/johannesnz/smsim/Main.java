package me.johannesnz.smsim;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
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

public class Main extends Service {

    private SharedPreferences prefs;
    private IDuplexStringMessageSender sender;
    private BroadcastReceiver messageReceiver;

    @Override
    public void onCreate() {
        prefs = getSharedPreferences("SMSIM", MODE_PRIVATE);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        messageReceiver = new Receiver();
        registerReceiver(messageReceiver, filter);
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
                        if (!sender.getAttachedDuplexOutputChannel().isConnected()) {
                            Intent broadcastIntent = new Intent();
                            broadcastIntent.setAction("me.johannesnz.UPDATE");
                            broadcastIntent.putExtra("update", "disconn");
                            sendBroadcast(broadcastIntent);
                            openConnection(ip);
                        } else {
                            Thread.sleep(300000);
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

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
                if (sender.getAttachedDuplexOutputChannel().isConnected()) {
                    Bundle bundle = intent.getExtras();
                    SmsMessage[] messages;
                    if (bundle != null) {
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        messages = new SmsMessage[pdus.length];
                        for (int i = 0; i < messages.length; i++) {
                            Log.i("Log", "Message recieved " + i);
                            messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                            String from = messages[i].getOriginatingAddress();
                            String body = messages[i].getMessageBody();
                            String displayName;
                            Uri lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(from));
                            Cursor c = context.getContentResolver().query(lookupUri, new String[]{ContactsContract.Data.DISPLAY_NAME}, null, null, null);
                            c.moveToFirst();
                            try {
                                displayName = c.getString(0);
                            } catch (CursorIndexOutOfBoundsException e) {
                                displayName = "Unknown";
                            }
                            try {
                                sender.sendMessage("SMS:" + displayName + ":" + from + ":" + body);
                                Log.i("Log", "Message sent to PC");
                                if (prefs.getBoolean("supressAlerts", false)) {
                                    abortBroadcast();
                                }
                            } catch (Exception e) {
                                Log.i("Log", "Message NOT sent to PC");
                                Intent broadcastIntent = new Intent();
                                broadcastIntent.setAction("me.johannesnz.UPDATE");
                                broadcastIntent.putExtra("update", "disconn");
                                sendBroadcast(broadcastIntent);
                            }
                        }
                    }
                } else {
                    Intent broadcastIntent = new Intent();
                    broadcastIntent.setAction("me.johannesnz.UPDATE");
                    broadcastIntent.putExtra("update", "disconn");
                    sendBroadcast(broadcastIntent);
                }
            } else {
                Log.i("Log", "Receiver fired, bad intent: " + intent.getAction());
            }
        }

        public Receiver() {

        }
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

    EventHandler<StringResponseReceivedEventArgs> handler = new EventHandler<StringResponseReceivedEventArgs>() {
        @Override
        public void onEvent(Object o, StringResponseReceivedEventArgs response) {
            if (response.getResponseMessage().startsWith("Ack: Conn")) {
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction("me.johannesnz.UPDATE");
                broadcastIntent.putExtra("update", "conn");
                sendBroadcast(broadcastIntent);
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                Notification not = new Notification(R.mipmap.ic_launcher, "SMSIM: Connected.", System.currentTimeMillis());
                Intent notificationIntent = new Intent(getApplicationContext(), Settings.class);
                PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_NO_CREATE);
                not.flags = Notification.FLAG_ONGOING_EVENT;
                not.setLatestEventInfo(getApplicationContext(), "SMSIM", "Service is running.", pi);
                mNotificationManager.notify(1, not);
                Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                while (phones.moveToNext()) {
                    int phoneType = phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                    if (phoneType == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) {
                        String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        try {
                            sender.sendMessage("Contact:" + name + ":" + phoneNumber);
                        } catch (Exception e) {
                            Log.e("Log", e.toString());
                        }
                    }
                }
                phones.close();
            }
            if (response.getResponseMessage().startsWith("SMS:")) {
                Log.i("Log", "Sending...");
                String[] input = response.getResponseMessage().split(":");
                SmsManager sms = SmsManager.getDefault();
                sms.sendTextMessage(input[1], null, input[2], null, null);
                Log.i("Log", "Sent.");
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
