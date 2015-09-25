package me.johannesnz.smsim;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.SmsMessage;

public class SMSReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (isMainServiceRunning(context) && Main.connected) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
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
                    Main.sendMessage("SMS:" + displayName + ":" + from + ":" + body);
                    if (prefs.getBoolean("suppressAlerts", false)) {
                        abortBroadcast();
                    }
                }
            }
        }
    }

    private boolean isMainServiceRunning(Context context) {
        Class<?> serviceClass = Main.class;
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
