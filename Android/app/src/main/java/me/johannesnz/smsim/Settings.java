package me.johannesnz.smsim;

import android.content.Context;
import android.os.Bundle;
import android.content.Intent;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.support.v7.app.AppCompatActivity;

public class Settings extends AppCompatActivity implements OnSharedPreferenceChangeListener {

    public static class SettingsFragment extends PreferenceFragment implements OnPreferenceClickListener {

        private static Context context;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            findPreference("exit").setOnPreferenceClickListener(this);
            findPreference("restart").setOnPreferenceClickListener(this);
            findPreference("kill").setOnPreferenceClickListener(this);
            findPreference("donate").setOnPreferenceClickListener(this);
            context = getActivity();
        }

        @Override
        public boolean onPreferenceClick(Preference pref) {
            switch (pref.getKey()) {
                case ("exit"):
                    Main.main.stopSelf();
                    break;
                case ("restart"):
                    Main.main.stopSelf();
                    Settings.startMainService(context);
                    break;
                case ("kill"):
                    android.os.Process.killProcess(android.os.Process.myPid());
                    break;
                case ("donate"):
                    // TODO
                    break;
            }
            return true;
        }
    }

    public static void startMainService(final Context context) {
        context.startService(new Intent(context, Main.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        startMainService(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals("ip")) {
            Main.main.stopSelf();
            startMainService(this);
        }
    }

}
