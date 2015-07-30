package com.cfap.cfadevicemanager;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

/**
 * Created by Shreya Jagarlamudi on 27/07/15.
 */

/**
 * This class is the main class for the entire application. As soon as the user launches application, oncreate method of this
 * class is triggered before anything else.
 * This class starts the MQTT service on launch first time we open the app ever and also contains UI elements to display
 * messages to the user. The UI will be updated from our CfaService class using Broadcast Receiver.
 */
public class MainActivity extends Activity {

    private String TAG = "MainActivity";
    private Intent intent;
    private TextView maintv;
    GlobalState gs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intent = new Intent(this, CFAService.class);
        gs = (GlobalState) getApplication();

        maintv = (TextView) findViewById(R.id.maintv);
        String display = "DETAILS: \n\nFetching Data...";
        maintv.setText(display+" "+gs.getjStr());
        maintv.setTextSize(20);

        // we only start service if it is already not running
       if(isMyServiceRunning(CFAService.class)==false){
           Log.e(TAG, "service not running, starting now!");
           startMqttService();
       }
    }

    public void startMqttService(){
        Intent intent = new Intent(MainActivity.this, CFAService.class);
        startService(intent);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI(intent);
        }
    };

    public void updateUI(Intent in){
        maintv.setTextSize(20);
        String text = in.getStringExtra("jstring");
        maintv.setText("DETAILS: \n\n"+gs.getjStr());
        maintv.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //startMqttService();
        registerReceiver(broadcastReceiver, new IntentFilter(CFAService.BROADCAST_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
