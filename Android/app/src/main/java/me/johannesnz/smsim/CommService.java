package me.johannesnz.smsim;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

    public static long lastPing;
    private SharedPreferences prefs;

    public CommService() {
        super("CommService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        String command = intent.getStringExtra("command");
        switch (command) {
            case "connect":
                if (!setUp()) connFail();
                break;
            case "send":
                String data = intent.getStringExtra("data");
                sendMessage(data);
                break;
            case "disconnect":
                break;
        }
    }

    private boolean setUp() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String ip = prefs.getString("ip", "");
        final IDuplexStringMessagesFactory factory = new DuplexStringMessagesFactory();
        final IMessagingSystemFactory messenger = new TcpMessagingSystemFactory();
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
            Main.connected = true;
            Main.showNotification(this, 1, "Connected.", true);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void sendMessage(String message) {
        try {
            Main.sender.sendMessage(message);
        } catch (Exception e) {
            connFail();
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
            else connFail();
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
            connFail();
        }
    }

    public void connFail() {
        Main.connected = false;
        Main.sender.detachDuplexOutputChannel();
    }

}
