package com.cfap.cfadevicemanager.services;

/**
 * Created by Shreya Jagarlamudi on 30/07/15.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.cfap.cfadevicemanager.dbmodels.DatabaseHelper;
import com.cfap.cfadevicemanager.GlobalState;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * This class continuously listens for network connection. If we are connected to the internet, it sets the global
 * variable connStatus to true and prints out the time. If the connection is lost, it prints out false along with the time.
 */
public class NetworkStateReceiver extends BroadcastReceiver {

    private String TAG = "NetworkStateReceiver";
    GlobalState gs;
    private DatabaseHelper myDbHelp;
    final int PROCESS_STATE_TOP = 2;
    private static Context ctx;

    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "Network connectivity change");
        ctx = context;
        gs = (GlobalState) context.getApplicationContext();
        myDbHelp = DatabaseHelper.getInstance(context);
        if(intent.getExtras()!=null) {
            NetworkInfo ni=(NetworkInfo) intent.getExtras().get(ConnectivityManager.EXTRA_NETWORK_INFO);
            if(ni!=null && ni.getState()==NetworkInfo.State.CONNECTED) {
                Log.e(TAG, "Network " + ni.getTypeName() + " connected");
                gs.setConnStatus("true");
                Log.e(TAG, "no of pending tasks: "+myDbHelp.getPendingJsons().size());
                if(myDbHelp.getPendingJsons().size()>0) new FetchFromDatabase(context, "myimei");
                Intent serviceIntent = new Intent(context, MyMqttService.class);
                serviceIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startService(serviceIntent);
            }
        }
        if(intent.getExtras().getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
            Log.d(TAG, "There's no network connectivity");
            gs.setConnStatus("false");
        }
    }
}