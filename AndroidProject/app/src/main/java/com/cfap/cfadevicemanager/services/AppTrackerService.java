package com.cfap.cfadevicemanager.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;

import com.cfap.cfadevicemanager.dbmodels.DataTrackerDBModel;
import com.cfap.cfadevicemanager.models.TrafficSnapshot;
import com.cfap.cfadevicemanager.utils.Intents;
import com.cfap.cfadevicemanager.utils.SharedPrefUtils;

/**
 * Created by PraveenKatha on 30/09/15.
 */
public class AppTrackerService extends IntentService {

    public AppTrackerService() {
        this("AppTrackerService");
    }

    public AppTrackerService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();

        boolean prevWifiStatus = SharedPrefUtils.getCurrentWifiStatus(getApplicationContext());
        TrafficSnapshot trafficSnapshot = TrafficSnapshot.getCurrentTrafficSnapshot(getApplicationContext());
        DataTrackerDBModel.addOrUpdateTrafficSnapshot(trafficSnapshot, getApplicationContext(), prevWifiStatus, action.equals(Intent.ACTION_SHUTDOWN));

        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            System.out.println("Wifi connection changed");
        } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            System.out.println("BOOT UP COMPLETED");
        } else if (action.equals(Intent.ACTION_SHUTDOWN)) {
            System.out.println("SHUT DOWN");
        } else if (action.equals(Intent.ACTION_DATE_CHANGED)) {
            System.out.println("DATE CHANGED");
        } else if (action.equals(Intents.APP_INSTALL_EVENT)) {
            System.out.println("FIRST TIME APP INSTALLED");
        }

<<<<<<< HEAD
=======

>>>>>>> 250970c2e8e3780a150eb8aca12cfa0e24e91c94
        ConnectivityManager connManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        SharedPrefUtils.updateCurrentWifiStatus(mWifi.isConnected(), getApplicationContext());

        LocalBroadcastManager.getInstance(this).sendBroadcast((new Intent(Intents.APP_DATA_UPDATED_EVENT)));
    }
}
