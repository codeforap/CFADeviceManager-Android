package com.cfap.cfadevicemanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Shreya Jagarlamudi on 03/09/15.
 */
public class CFAReceiver extends BroadcastReceiver{

    private String TAG = "CFAReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent!=null) {
            String type = intent.getStringExtra("serviceType");

            if (type.equals("Location")) {
                Intent service1 = new Intent(context, LocationService.class);
                context.startService(service1);
                Log.e(TAG, "type is Location");
            } else if (type.equals("AppUsage")) {
                Intent service1 = new Intent(context, AppUsageService.class);
                context.startService(service1);
                Log.e(TAG, "type is AppUsage");
            }else if (type.equals("Foreground")) {
                Intent service1 = new Intent();
                service1.setAction("com.cfap.CUSTOM_INTENT");
                context.sendBroadcast(service1);
                // Log.e(TAG, "type is Foreground");
            }
        }
    }
}
