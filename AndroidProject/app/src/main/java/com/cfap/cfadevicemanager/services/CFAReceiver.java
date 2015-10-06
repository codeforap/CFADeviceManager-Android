package com.cfap.cfadevicemanager.services;

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
            } else if (type.equals("DataUsage")) {
                Intent service1 = new Intent(context, DataUsageService.class);
                context.startService(service1);
                Log.e(TAG, "type is DataUsage");
            }else if (type.equals("Foreground")) {
                Intent service1 = new Intent();
                service1.setAction("com.cfap.CUSTOM_INTENT");
                context.sendBroadcast(service1);
                Log.e(TAG, "type is Foreground");
            }
        }
    }
}
