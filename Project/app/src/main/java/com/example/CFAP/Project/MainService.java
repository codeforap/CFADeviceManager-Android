package com.example.CFAP.Project;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by anilreddy on 23/7/15.
 */


public class MainService extends  Service {
    public static final String Server = "tcp://104.155.237.100:1883";
    public static  String Client ;
    public static  String Topic ;
    public static MqttClient mqttClient;

    public String KEY_IMEI = "imei";
    public String KEY_Battery = "battery";
    public String KEY_Location_Lat = "locationlat";
    public String KEY_Location_Long = "locationlong";

    public String imei = "";
    public String battery = "";
    public String locationlat = "";
    public String locationlong = "";

    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onStart(Intent intent, int startId) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            Client = telephonyManager.getDeviceId().toString();
            // Client="111";
            Topic=Client;
            mqttClient = new MqttClient(Server, Client, new MemoryPersistence());
            mqttClient.setCallback(new PushCallback(this));
            mqttClient.connect();
            mqttClient.subscribe("Details",2);

        } catch (MqttException e) {
            System.out.println("Error");
            Toast.makeText(getApplicationContext(), "Something went wrong!" + e.getMessage().toString(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        super.onStart(intent, startId);

    }



    @Override
    public void onDestroy() {
        try {
            mqttClient.disconnect(0);
        } catch (MqttException e) {
            Toast.makeText(getApplicationContext(), "Something went wrong!" + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}



