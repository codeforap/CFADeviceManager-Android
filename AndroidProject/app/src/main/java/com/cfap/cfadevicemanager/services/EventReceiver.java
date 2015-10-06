package com.cfap.cfadevicemanager.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Praveen Katha on 30/09/15.
 */
public class EventReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println(intent.getAction());

        Intent i = new Intent(context,AppTrackerService.class);
        i.setAction(intent.getAction());
        context.startService(i);
    }
}
