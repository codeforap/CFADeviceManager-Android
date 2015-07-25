package com.example.anilreddy.projecty;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;


public class PushCallback implements MqttCallback {

    private ContextWrapper context;

    public PushCallback(ContextWrapper context) {

        this.context = context;
    }

    @Override
    public void connectionLost(Throwable cause) {
        //We should reconnect here
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

       NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);



        Notification notification = new Notification(R.drawable.ic_action,
                "New Messages", System.currentTimeMillis());



        // Hide the notification after its selected
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        final Intent intent = new Intent(context, MainActivity.class);

        final PendingIntent activity = PendingIntent.getActivity(context, 0, intent, 0);
        notification.setLatestEventInfo(context, s ,new String(mqttMessage.getPayload()), activity);
        notification.number += 1;
        notificationManager.notify((int) System.currentTimeMillis(), notification);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }


}