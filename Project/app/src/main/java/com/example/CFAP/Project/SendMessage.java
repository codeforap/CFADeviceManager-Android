package com.example.CFAP.Project;

import android.content.Context;
import android.telephony.TelephonyManager;

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
public class SendMessage {
    public static final String Server = "tcp://104.199.134.192:1883";
    public static MqttClient mqttClient;


    public String KEY_IMEI = "imei";
    public String KEY_Battery = "battery";
    public String KEY_Location_Lat = "locationlat";
    public String KEY_Location_Long = "locationlong";

    public String imei = "";
    public String battery = "";
    public String locationlat = "";
    public String locationlong = "";
    public void sendMessage1() throws MqttException {


        mqttClient = new MqttClient(Server, "123", new MemoryPersistence());
        mqttClient.connect();

        try {
            String ms = MessageFormat1();
            final MqttMessage message = new MqttMessage(ms.getBytes());
            final byte[] b = message.getPayload();
            mqttClient.publish("Detailsprojecty", b, 2, false);

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (MqttPersistenceException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }


    public String MessageFormat1() throws JSONException {
        String returning;
        JSONObject json = new JSONObject();

        imei = MainActivity.imei;

        locationlat = MainActivity.latitude + "";
        locationlong = MainActivity.longitude + "";
        battery = MainActivity.battery+"";

        json.put(KEY_IMEI, imei);
        json.put(KEY_Battery, battery);
        json.put(KEY_Location_Lat, locationlat);
        json.put(KEY_Location_Long, locationlong);

        returning = json.toString();
        System.out.println("Json String " + returning);
        return returning;
    }
}