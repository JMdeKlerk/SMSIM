package me.johannesnz.smsim;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;

import eneter.messaging.endpoints.stringmessages.DuplexStringMessagesFactory;
import eneter.messaging.endpoints.stringmessages.IDuplexStringMessagesFactory;
import eneter.messaging.endpoints.stringmessages.StringResponseReceivedEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.net.system.EventHandler;

public class CommService extends IntentService {

    private ArrayList<String> failedMessages = new ArrayList();

    public CommService() {
        super("CommService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        String data, command = intent.getStringExtra("command");
        switch (command) {
            case "connect":
                if (!setUp()) connFail("CONNECTION REFUSED");
                break;
            case "send":
                data = intent.getStringExtra("data");
                sendMessage(data, true);
                break;
            case "disconnect":
                data = intent.getStringExtra("data");
                connFail(data);
                break;
        }
    }

    private boolean setUp() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (!wifi.isConnected()) return false;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String ip = prefs.getString("ip", "");
        IDuplexStringMessagesFactory factory = new DuplexStringMessagesFactory();
        IMessagingSystemFactory messenger = new TcpMessagingSystemFactory();
        Main.sender = factory.createDuplexStringMessageSender();
        try {
            IDuplexOutputChannel output = messenger.createDuplexOutputChannel("tcp://" + ip + ":8060/");
            Main.sender.attachDuplexOutputChannel(output);
            Main.sender.responseReceived().subscribe(new EventHandler<StringResponseReceivedEventArgs>() {
                @Override
                public void onEvent(Object o, StringResponseReceivedEventArgs response) {
                    handleResponse(response.getResponseMessage());
                }
            });
            sendMessage("Version", true);
        } catch (Exception ex) {
            return false;
        }
        Main.setConnected(this, true);
        Main.showNotification(this, 1, "Connected.", true);
        AlarmManager aManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, RetryAlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        aManager.cancel(alarmIntent);
        return true;
    }

    private void sendMessage(String message, boolean handleFail) {
        try {
            for (String retryMessage : failedMessages) {
                Main.sender.sendMessage(retryMessage);
            }
            Main.sender.sendMessage(message);
        } catch (Exception e) {
            if (handleFail) {
                failedMessages.add(message);
                connFail("SEND FAILED");
            }
        }
    }

    private void handleResponse(String message) {
        Main.lastPing = System.currentTimeMillis();
        if (message.split(":")[1].equals("Ping")) {
            sendMessage("Pong", true);
        }
        if (message.split(":")[1].equals("Version")) {
            String version = message.split(":")[2];
            if (version.equals("1.1")) sendMessage("Conn:" + Build.MODEL, true);
            else connFail("VERSION MISMATCH");
        }
        if (message.split(":")[1].equals("Contacts")) {
            Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            while (phones.moveToNext()) {
                int phoneType = phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                if (phoneType == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) {
                    String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    sendMessage("Contact:" + name + ":" + phoneNumber, true);
                }
            }
            phones.close();
        }
        if (message.split(":")[1].equals("SMS")) {
            Log.i("Log", message);
            String[] input = message.split(":");
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(input[3], null, input[4], null, null);
            sendMessage("Success:" + input[2], true);
        }
        if (message.split(":")[1].equals("DC")) {
            connFail("REMOTE DISCONNECT");
        }
    }

    public void connFail(String status) {
        if (!Main.isConnected(this)) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Main.setConnected(this, false);
        switch (status) {
            case "QUIT":
                sendMessage("DC", false);
                Main.sender = null;
                break;
            case "SEND FAILED":
            case "PING TIMEOUT":
            case "REMOTE DISCONNECT":
                Main.showNotification(this, 1, "Connection lost.", false);
                if (prefs.getBoolean("autoRetry", false)) retryLoop();
                break;
            case "CONNECTION REFUSED":
                Main.showNotification(this, 1, "Connection refused. Ensure PC client is running and check the IP address.", false);
                if (prefs.getBoolean("autoRetry", false)) retryLoop();
                break;
            case "VERSION MISMATCH":
                Main.showNotification(this, 1, "PC client is outdated - please download the latest version.", false);
                break;
        }
    }

    private synchronized void retryLoop() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        AlarmManager aManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, RetryAlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        int interval = Integer.parseInt(prefs.getString("autoRetryInterval", "300")) * 1000;
        aManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, interval, interval, alarmIntent);
    }

}
