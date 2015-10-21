package me.johannesnz.smsim;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.android.vending.util.IabHelper;
import com.android.vending.util.IabResult;

public class DonationActivity extends AppCompatActivity {

    IabHelper billingHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyesaLxbMV4dFvMQ" +
                "s3uefeEwri0NBJG6+OPKVVQNwkOHL9l/yyBDHYEZhwM8SZXqIIbsfniM33itgTZad/Xguz+kcxpJ76txGiS" +
                "DX/iK5i0C6y6vat6NiEb0EvHdvveew+gbbixUNnbCbKNBXNJoxYjD+ySZzDS+o4aomCwKcxsnjaefivuRh1" +
                "pWAupRGPF1sfu5N2htTr6c8LvHDVg00OIhMY8DcD/wVo5TYhkikOJXzw35hLvoQVplj4zQP2PnE7jYngUms" +
                "zSwfCkQD3bb8wunvju0JhwrNTqDTVB84W90AgxW5xv6I3ieafYbc1tLznlBvaocCze01+fli+721OQIDAQAB";
        billingHelper = new IabHelper(this, base64EncodedPublicKey);
        billingHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess())
                    Log.i("Log", "Problem setting up In-app Billing: " + result);
            }
        });
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (billingHelper != null) billingHelper.dispose();
        billingHelper = null;
    }

}
