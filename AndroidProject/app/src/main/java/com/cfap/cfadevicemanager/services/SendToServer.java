package com.cfap.cfadevicemanager.services;

/**
 * Created by Shreya Jagarlamudi on 03/09/15.
 */

import android.content.Context;
import android.util.Log;

import com.cfap.cfadevicemanager.DatabaseHelper;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import java.text.SimpleDateFormat;

/**
 * This class takes a json and sends it to server
 */
public class SendToServer{

    private String TAG = "SendToServer";
    private Context context;
    private DatabaseHelper myDbHelp;
    private String connTime = "";
    private String clientID;
    private String Server = "tcp://208.74.179.90:1883";
    private MqttClient mqttClient;
    private String topic;

    public SendToServer(Context c, JSONObject json, String topic) throws MqttException{
        context = c;
        this.topic = topic;
        myDbHelp = DatabaseHelper.getInstance(context);
        send(json);
    }

    private void send(JSONObject jsonObj) throws MqttException {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a");
        ISTDateTime ist = new ISTDateTime();
        connTime = formatter.format(ist.getIST());
        clientID = myDbHelp.getImei()+connTime;
            mqttClient = new MqttClient(Server, clientID, new MemoryPersistence());
       // Log.e(TAG, "connecting...");
            mqttClient.connect();
                String jString = jsonObj.toString();
                final MqttMessage message = new MqttMessage(jString.getBytes());
                final byte[] b = message.getPayload();
         //   Log.e(TAG, "publishing...");
                mqttClient.publish(topic, b, 2, false);
    }
}
