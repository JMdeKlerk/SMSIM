package me.johannesnz.smsim;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.InputStream;

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
                if (!setUp()) connFail("CONNECTION REFUSED");
                break;
            case "send":
                data = intent.getStringExtra("data");
                sendMessage(data);
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
            sendMessage("Version");
            Main.setConnected(this, true);
            Main.showNotification(this, 1, "Connected.", true);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void sendMessage(final String message) {
        try {
            Main.sender.sendMessage(message);
        } catch (Exception e) {
            connFail("SEND FAILED");
        }
    }

    private void handleResponse(String message) {
        Main.lastPing = System.currentTimeMillis();
        Log.i("Log", message);
        if (message.split(":")[1].equals("Ping")) {
            sendMessage("Pong");
        }
        if (message.split(":")[1].equals("Version")) {
            String version = message.split(":")[2];
            if (version.equals("1.0")) sendMessage("Conn:" + Build.MODEL);
            else connFail("VERSION MISMATCH");
        }
        if (message.split(":")[1].equals("Contacts")) {
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
        if (message.split(":")[1].equals("SMS")) {
            String[] input = message.split(":");
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(input[2], null, input[3], null, null);
            sendMessage("Success:" + input[0]);
        }
        if (message.split(":")[1].equals("DC")) {
            connFail("REMOTE DISCONNECT");
        }
    }

    public void connFail(String status) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Main.setConnected(this, false);
        switch (status) {
            case "QUIT":
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
                Main.showNotification(this, 2, "PC client is outdated - please download the latest version.", false);
                break;
        }
    }

    private void retryLoop() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        while (!Main.isConnected(this) && prefs.getBoolean("autoRetry", false) && !setUp()) {
            android.os.SystemClock.sleep(Integer.parseInt(prefs.getString("autoRetryInterval", "300")) * 1000);
        }
    }

}
