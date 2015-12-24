package me.johannesnz.smsim;

import android.app.Activity;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.preference.PreferenceManager;
import android.util.Log;

import eneter.messaging.endpoints.stringmessages.DuplexStringMessagesFactory;
import eneter.messaging.endpoints.stringmessages.IDuplexStringMessagesFactory;
import eneter.messaging.endpoints.stringmessages.StringResponseReceivedEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.net.system.EventHandler;

public class CommService extends IntentService {

    public CommService() {
        super("CommService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        String data, command = intent.getStringExtra("command");
        switch (command) {
            case "connect":
                Main.setConnected(this, true);
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
        Main.cancelAutoRetryAlarm(this);
        Main.setPingAlarm(this);
        Main.showNotification(this, "Connected.", true);
        return true;
    }

    private void sendMessage(String message, boolean handleFail) {
        try {
            if (message.equals("Version")) Main.sender.sendMessage(message);
            else {
                int id = Main.getUniqueId(this);
                Main.sender.sendMessage(id + ":" + message);
            }
        } catch (Exception e) {
            if (handleFail) connFail("SEND FAILED");
        }
    }

    private void handleResponse(String message) {
        Log.i("Log", message);
        Main.lastPing = System.currentTimeMillis();
        if (message.split(":")[1].equals("Ping")) {
            sendMessage("Pong", true);
        }
        if (message.split(":")[1].equals("Version")) {
            String version = message.split(":")[2];
            if (version.equals("1.1")) sendMessage("Conn:" + Build.MODEL, true);
            else {
                sendMessage("Mismatch", false);
                connFail("VERSION MISMATCH");
            }
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
            String[] input = message.split(":");
            SmsManager sms = SmsManager.getDefault();
            Intent intent = new Intent(input[2]);
            PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, intent, 0);
            this.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (getResultCode() == Activity.RESULT_OK) sendMessage("Success:" + intent.getAction(), true);
                    else {
                        sendMessage("Fail:" + intent.getAction(), true);
                        Log.i("Log", "Failed send. Error code " + String.valueOf(getResultCode()));
                    }
                    unregisterReceiver(this);
                }
            }, new IntentFilter(input[2]));
            sms.sendTextMessage(input[3], null, input[4], sentPI, null);
        }
        if (message.split(":")[1].equals("DC")) {
            connFail("REMOTE DISCONNECT");
        }
    }

    public void connFail(String status) {
        if (!Main.isConnected(this) || setUp()) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Main.setConnected(this, false);
        Main.cancelPingAlarm(this);
        Log.i("Log", status);
        switch (status) {
            case "QUIT":
                sendMessage("DC", false);
                Main.sender = null;
                break;
            case "SEND FAILED":
            case "PING TIMEOUT":
            case "REMOTE DISCONNECT":
                Main.showNotification(this, "Connection lost.", false);
                if (prefs.getBoolean("autoRetry", false)) Main.setAutoRetryAlarm(this);
                break;
            case "CONNECTION REFUSED":
                Main.showNotification(this, "Connection refused.", false);
                if (prefs.getBoolean("autoRetry", false)) Main.setAutoRetryAlarm(this);
                break;
            case "VERSION MISMATCH":
                Main.showNotification(this, "PC client out of date.", false);
                break;
        }
    }

}
