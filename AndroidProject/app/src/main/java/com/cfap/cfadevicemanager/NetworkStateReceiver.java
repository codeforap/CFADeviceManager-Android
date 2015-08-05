package com.cfap.cfadevicemanager;

/**
 * Created by Shreya Jagarlamudi on 30/07/15.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class continuously listens for network connection. If we are connected to the internet, it sets the global
 * variable connStatus to true and prints out the time. If the connection is lost, it prints out false along with the time.
 */
public class NetworkStateReceiver extends BroadcastReceiver {

    private String TAG = "NetworkStateReceiver";
    GlobalState gs;

    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "Network connectivity change");
        gs = (GlobalState) context.getApplicationContext();
        if(intent.getExtras()!=null) {
            NetworkInfo ni=(NetworkInfo) intent.getExtras().get(ConnectivityManager.EXTRA_NETWORK_INFO);
            if(ni!=null && ni.getState()==NetworkInfo.State.CONNECTED) {
                Log.e(TAG,"Network "+ni.getTypeName()+" connected");
           //     SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
            //    String connTime = formatter.format(new Date());
                gs.setConnStatus("true");
            }
        }
        if(intent.getExtras().getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY,Boolean.FALSE)) {
            Log.d(TAG, "There's no network connectivity");
        //    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
        //    String connTime = formatter.format(new Date());
            gs.setConnStatus("false");
        }
    }
}