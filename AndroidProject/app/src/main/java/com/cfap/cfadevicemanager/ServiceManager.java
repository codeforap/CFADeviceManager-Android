package com.cfap.cfadevicemanager;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by Shreya Jagarlamudi on 27/07/15.
 */

/**
 * This class listens to Boot completed action i.e when the user restarts the device, it restarts our CFAService
 */
public class ServiceManager extends BroadcastReceiver {

    Context mContext;
    private final String BOOT_ACTION = "android.intent.action.BOOT_COMPLETED";
    private SharedPreferences sharedpreferences;
    private final String MyPREFERENCES = "CfaPrefs" ;
    private final String usernamePref = "cfausername";
    private final String passwordPref = "cfapassword";

    @Override
    public void onReceive(Context context, Intent intent) {
        // All registered broadcasts are received by this
        mContext = context;
        sharedpreferences = mContext.getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        String action = intent.getAction();
        if (action.equalsIgnoreCase(BOOT_ACTION)) {
            Log.e("ServiceManager", "device boot");
            if(sharedpreferences.getString(usernamePref, "noValun").equals("admin") &&
                    sharedpreferences.getString(passwordPref, "noValpw").equals("cfap")) {
                //check for boot complete event & start your service
                if (isMyServiceRunning(CFAService.class) == false) {
                    Log.e("ServiceManager", "Restarting service in device boot");
                    startService();
                }
            }
        }

    }

    private void startService() {
        //here, you will start your service
        Intent mServiceIntent = new Intent();
        mServiceIntent.setAction("com.cfap.cfadevicemanager.CFAService");
        if(mContext==null) Log.e("ServiceManager", "mcontext is null");
        mContext.startService(mServiceIntent);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}