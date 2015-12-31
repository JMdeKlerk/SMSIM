package me.johannesnz.smsim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.SmsMessage;

public class SMSReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (Main.isConnected(context)) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.getBoolean("suppressAlerts", false) || intent.getBooleanExtra("fake", false)) abortBroadcast();
            Bundle bundle = intent.getExtras();
            Intent fakeIntent = new Intent("android.provider.Telephony.SMS_RECEIVED");
            fakeIntent.putExtras(bundle);
            fakeIntent.putExtra("fake", true);
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                SmsMessage[] messages = new SmsMessage[pdus.length];
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
                    String message = "SMS:" + displayName + ":" + from + ":" + body;
                    Main.sendMessageAndStoreIntent(context, message, fakeIntent);
                }
            }
            Thread retryThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    SystemClock.sleep(3000);
                    for (Intent failedMessage : Main.pendingMessages) {
                        if (failedMessage != null) {
                            context.sendOrderedBroadcast(failedMessage, "android.permission.RECEIVE_SMS");
                            Main.pendingMessages.set(Main.pendingMessages.indexOf(failedMessage), null);
                        }
                    }
                }
            });
            retryThread.start();
        }
    }

}
