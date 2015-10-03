package com.cfap.cfadevicemanager.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Shreya Jagarlamudi on 02/10/15.
 */
public class BootCompleteReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, MyMqttService.class);
        context.startService(service);
    }
}
