package me.johannesnz.smsim;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.android.vending.util.IabHelper;
import com.android.vending.util.IabResult;
import com.android.vending.util.Purchase;

public class DonationActivity extends AppCompatActivity {

    IabHelper billingHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyesaLxbMV4dFvMQ" +
                "s3uefeEwri0NBJG6+OPKVVQNwkOHL9l/yyBDHYEZhwM8SZXqIIbsfniM33itgTZad/Xguz+kcxpJ76txGiS" +
                "DX/iK5i0C6y6vat6NiEb0EvHdvveew+gbbixUNnbCbKNBXNJoxYjD+ySZzDS+o4aomCwKcxsnjaefivuRh1" +
                "pWAupRGPF1sfu5N2htTr6c8LvHDVg00OIhMY8DcD/wVo5TYhkikOJXzw35hLvoQVplj4zQP2PnE7jYngUms" +
                "zSwfCkQD3bb8wunvju0JhwrNTqDTVB84W90AgxW5xv6I3ieafYbc1tLznlBvaocCze01+fli+721OQIDAQAB";
        billingHelper = new IabHelper(this, base64EncodedPublicKey);
        billingHelper.startSetup(null);
        setContentView(R.layout.donate);
    }

    public void donateMin(View view) {
        billingHelper.launchPurchaseFlow(this, "donate_1", 0, donationFinishedListener);
    }

    public void donateMed(View view) {
        billingHelper.launchPurchaseFlow(this, "donate_2", 0, donationFinishedListener);
    }

    public void donateMax(View view) {
        billingHelper.launchPurchaseFlow(this, "donate_3", 0, donationFinishedListener);
    }

    IabHelper.OnIabPurchaseFinishedListener donationFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase info) {
            if (result.isSuccess()) Log.i("Log", info.getSku() + " purchased");
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (billingHelper != null) billingHelper.dispose();
        billingHelper = null;
    }

}
