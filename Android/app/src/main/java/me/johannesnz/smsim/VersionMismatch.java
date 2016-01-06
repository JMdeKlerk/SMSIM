package me.johannesnz.smsim;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

public class VersionMismatch extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.versionmismatch);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        this.getWindow().setAttributes(params);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPause() {
        finishAffinity();
        super.onPause();
    }

    public void exit(View view) {
        finishAffinity();
    }

}