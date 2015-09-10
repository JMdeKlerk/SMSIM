package me.johannesnz.smsim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsMessage;

public class SMSReceiver extends BroadcastReceiver {

    Main parent;
    private SharedPreferences prefs;

    public SMSReceiver(Main parent) {
        this.parent = parent;
        prefs = parent.getSharedPreferences("SMSIM", Context.MODE_PRIVATE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (prefs.getBoolean("isConnected", false)) {
            Bundle bundle = intent.getExtras();
            SmsMessage[] messages;
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                messages = new SmsMessage[pdus.length];
                for (int i = 0; i < messages.length; i++) {
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
                        parent.sender.sendMessage("SMS:" + displayName + ":" + from + ":" + body);
                        if (prefs.getBoolean("supressAlerts", false)) {
                            abortBroadcast();
                        }
                    } catch (Exception e) {
                        Intent broadcastIntent = new Intent();
                        broadcastIntent.setAction("me.johannesnz.UPDATE");
                        broadcastIntent.putExtra("update", "disconn");
                        context.sendBroadcast(broadcastIntent);
                    }
                }
            }
        }
    }
}
