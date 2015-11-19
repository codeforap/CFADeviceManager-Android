package com.cfap.cfadevicemanager.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.Ringtone;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.MessageQueue;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.cfap.cfadevicemanager.dbmodels.DatabaseHelper;
import com.cfap.cfadevicemanager.DialogActivity;
import com.cfap.cfadevicemanager.models.AndroidAgentException;
import com.cfap.cfadevicemanager.models.ApplicationManager;
import com.cfap.cfadevicemanager.models.DeviceAppInfo;
import com.cfap.cfadevicemanager.models.DeviceInfo;
import com.cfap.cfadevicemanager.models.DeviceState;
import com.cfap.cfadevicemanager.models.WiFiConfig;
import com.cfap.cfadevicemanager.utils.Constants;
import com.cfap.cfadevicemanager.utils.DeviceStateFactory;
import com.cfap.cfadevicemanager.utils.Functions;

import org.eclipse.paho.android.service.MqttService;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Shreya Jagarlamudi on 18/09/15.
 */
public class MyMqttService extends Service{

    private static String TAG = "MyMqttService";
    public static final String BROKER_URL = "tcp://208.74.179.90:1883";
    public static DatabaseHelper myDbHelp;
    public static String clientId;
    public static String TOPIC;
    public static  MqttClient mqttClient;
    public SQLiteDatabase db;
    public  static MqttConnectOptions options;
    public static  String  pastlogged = null;
    public static  String log  = null;
    public static int keepAliveSeconds = 60*20;
   // public static  int  sss = 0;

    public IBinder onBind(Intent intent) {
        return null;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        System.out.println("onstartcommand MyMqttService");
        myDbHelp = DatabaseHelper.getInstance(this);
        TOPIC = "id"+myDbHelp.getImei();

   /*     if(pastlogged!=null)
        {
            if(pastlogged.equals(log))
            {

            }
            else
            {
                sss=0;
                pastlogged = log;
            }
        }
        else
        {
            pastlogged = log;
        }
        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
       */

        clientId="client"+myDbHelp.getImei();

        try {
        //    if(sss==0) {
                mqttClient = new MqttClient(BROKER_URL, clientId, new MemoryPersistence());
                mqttClient.setCallback(new PushCallback1(this));
                options = new MqttConnectOptions();
                options.setCleanSession(false);
                options.setKeepAliveInterval(keepAliveSeconds);
                mqttClient.connect(options);

                System.out.println("-----------------Connected111----------------------");
                //Subscribe to all subtopics of homeautomation
          //      String mod_code = "shreyaj";
                System.out.println("Subscribing to "+TOPIC);
                mqttClient.subscribe(TOPIC, 2);
            //    sss = sss +1;
                //  Toast.makeText(getApplicationContext(), "connected to server", Toast.LENGTH_LONG).show();
        //    }
          //  else if(sss != 0)
           // {
                if(!mqttClient.isConnected()){
                    System.out.println("-----------------disConnected----------------------");
                    mqttClient.setCallback(new PushCallback1(this));
                    mqttClient.connect(options);
                    //MainActivity.connectedflag=1;
           //     }
            }

        } catch (MqttException e) {
            //  Toast.makeText(getApplicationContext(), "Something went wrong!" + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }


    public static void publishToServer(JSONObject json, String topic) throws MqttException {
        if(!mqttClient.isConnected()) {
            Log.e(TAG, "connecting to Mqtt publish...");
            mqttClient.connect(MyMqttService.options);
        }
        String jString = json.toString();
        final MqttMessage message = new MqttMessage(jString.getBytes());
        final byte[] b = message.getPayload();
        Log.e(TAG, "publishing...");
        mqttClient.publish(topic, b, 2, false);
    }

    public static void disconnectClient(){
        if(mqttClient.isConnected()){
            try {
                mqttClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    /*
    * Schedule the next time that you want the phone to wake up and ping the
    *  message broker server
    */
/*    private void scheduleNextPing()
    {
        // When the phone is off, the CPU may be stopped. This means that our
        //   code may stop running.
        // When connecting to the message broker, we specify a 'keep alive'
        //   period - a period after which, if the client has not contacted
        //   the server, even if just with a ping, the connection is considered
        //   broken.
        // To make sure the CPU is woken at least once during each keep alive
        //   period, we schedule a wake up to manually ping the server
        //   thereby keeping the long-running connection open
        // Normally when using this Java MQTT client library, this ping would be
        //   handled for us.
        // Note that this may be called multiple times before the next scheduled
        //   ping has fired. This is good - the previously scheduled one will be
        //   cancelled in favour of this one.
        // This means if something else happens during the keep alive period,
        //   (e.g. we receive an MQTT message), then we start a new keep alive
        //   period, postponing the next ping.

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(MQTT_PING_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // in case it takes us a little while to do this, we try and do it
        //  shortly before the keep alive period expires
        // it means we're pinging slightly more frequently than necessary
        Calendar wakeUpTime = Calendar.getInstance();
        wakeUpTime.add(Calendar.SECOND, keepAliveSeconds);

        AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        aMgr.set(AlarmManager.RTC_WAKEUP,
                wakeUpTime.getTimeInMillis(),
                pendingIntent);
    } */

    @Override
    public void onDestroy() {
        try {
            System.out.println("nullllllllllllllllllllllllllllll/////////////////////////////lll");

            mqttClient.disconnect(0);
        } catch (MqttException e) {
            // Toast.makeText(getApplicationContext(), "Something went wrong!" + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private class  PushCallback1 implements MqttCallback {
        private ContextWrapper context;
        public  String KEY_Type = "type";
        public  String KEY_GeneratedId = "generatedid";
        public NotificationManager notificationManager;
        public Notification notification;


        public PushCallback1(ContextWrapper context) {

            this.context = context;
        }


        @Override
        public void connectionLost(Throwable throwable) {
            System.out.println("------------------Connection lost-----------------------");
            // if network connected, keep trying to reconnected until connected again
           if(isNetworkAvailable()==true){
               Log.e(TAG, "connLost network available");
               while(mqttClient.isConnected()==false){
                   try {
                       Log.e(TAG, "trying to reconnect to mqtt");
                    //   if(!MyMqttService.mqttClient.isConnected()){
                           MyMqttService.mqttClient.connect(MyMqttService.options);
                           System.out.println("connLost Subscribing to "+TOPIC);
                           mqttClient.subscribe(TOPIC, 2);
                   //    }

                   } catch (MqttException e) {
                       // TODO Auto-generated catch block
                       e.printStackTrace();
                   }
               }
           }
          /*  try {
                MyMqttService.mqttClient.connect(MyMqttService.options);
            } catch (MqttException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } */

        }

        @Override
        public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
            final JSONObject jsonObject = new JSONObject(new String( mqttMessage.getPayload()));
            Log.e(TAG, "mqtt message arrived"+jsonObject.toString());
            new Functions(MyMqttService.this, jsonObject);
        }


        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }

        private boolean isNetworkAvailable() {
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null;
        }

    }

}
