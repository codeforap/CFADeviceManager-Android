package com.cfap.cfadevicemanager.services;

/**
 * Created by Shreya Jagarlamudi on 14/6/15.
 */
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.util.Log;

import com.cfap.cfadevicemanager.utils.Functions;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;


public class PushCallback implements MqttCallback {

    private ContextWrapper context;

    public PushCallback(ContextWrapper context) {

        this.context = context;
    }


    @Override
    public void connectionLost(Throwable throwable) {
        //restarts Mqtt service which inturn reconnects and subscribes to Mqtt broker & topic
        Log.e("PushCallBack", "connectionLost in callBack");
      /*  Intent serviceIntent = new Intent(context, MyMqttService.class);
        context.stopService(serviceIntent);
        context.startService(serviceIntent); */
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

        System.out.println("Message arrived: "+mqttMessage.toString());
        final JSONObject jsonObject = new JSONObject(new String( mqttMessage.getPayload()));
        new Functions(context, jsonObject);

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }


}
