package com.cfap.cfadevicemanager.services;

import android.app.AlarmManager;
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
import android.media.AudioManager;
import android.media.Ringtone;
import android.net.Uri;
import android.os.IBinder;
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
/*
    private String TAG = "MyMqttService";
    private DatabaseHelper myDbHelp;
    private ISTDateTime ist;
    private String clientId;
    private String SERVER = "tcp://208.74.179.90:1883";
    private String topic="myimei";
    public static MqttClient mqttClient;
    public static MqttConnectOptions mqttOptions;
    private DeviceInfo deviceInfo;
    private DeviceState phoneState;
    private GPSTracker gps;
    private DevicePolicyManager devicePolicyManager;
    private Uri defaultRingtoneUri;
    private Ringtone defaultRingtone;
    private ApplicationManager appList;
    private AlarmManager alarmManager; */

    public MyMqttService(){

    }

    public MyMqttService(Context c){
       // this.context = c;
    }

    public static final String URL = "tcp://208.74.179.90:1883";
    //public static final String URL = "tcp://test.mosquitto.org:1883";
    public String clientId = "3453641234";
    public static final String TOPIC = "myimei";
    public static MqttClient mqttClient;


    public IBinder onBind(Intent intent) {
        return null;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            mqttClient = new MqttClient(URL, clientId, new MemoryPersistence());
            System.out.println("YES1");
            mqttClient.setCallback(new PushCallback(this));
            System.out.println("YES2");
            mqttClient.connect();
            System.out.println("YES3");
            mqttClient.subscribe(TOPIC);
            System.out.println("YES4");

       /*     String jString = "{\"command\": \"INSTALL_NEW_APP\", \"url\": \"http://www.codeforap.org/Spree.apk\", \"type\": \"enterprise\"}";
            try {
                JSONObject jsonObject = new JSONObject(jString);
                if(jsonObject.getString("command").equals("INSTALL_NEW_APP")){
                    Log.e("MyMqttService", "Command: " + "INSTALL_NEW_APP");
                    new Functions(MyMqttService.this, jsonObject);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } */

        } catch (MqttException e) {
            Toast.makeText(getApplicationContext(), "Something went wrong!" + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        return START_STICKY;
    }

    /* @Override
    public void onCreate() {
        super.onCreate();
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "MQTT service onstartcommand");
        this.devicePolicyManager =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.appList = new ApplicationManager(getApplicationContext());
        deviceInfo = new DeviceInfo(getApplicationContext());
        phoneState = DeviceStateFactory.getDeviceState(getApplicationContext(),
                deviceInfo.getSdkVersion());
        gps = new GPSTracker(getApplicationContext());
        myDbHelp = DatabaseHelper.getInstance(getApplicationContext());
        ist = new ISTDateTime();
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a");
            ISTDateTime ist = new ISTDateTime();
            String connTime = formatter.format(ist.getIST());
            clientId = myDbHelp.getImei()+" "+connTime;
            mqttClient = new MqttClient(SERVER, clientId, new MemoryPersistence());
            mqttClient.setCallback(new MyCallBack(this));
            mqttOptions = new MqttConnectOptions();
            mqttOptions.setCleanSession(false);
            mqttOptions.setKeepAliveInterval(1000000000);
            mqttClient.connect(mqttOptions);
            mqttClient.subscribe("myimei", 2);
            Log.e(TAG, "MQTT service created");
        } catch (MqttException e) {
            e.printStackTrace();
        }

        String jString = "{\"command\": \"INSTALL_NEW_APP\", \"url\": \"http://www.codeforap.org/Spree.apk\", \"type\": \"enterprise\"}";
        try {
            JSONObject jsonObject = new JSONObject(jString);
            if(jsonObject.getString("command").equals("INSTALL_NEW_APP")){
                Log.e(TAG, "Command: " + "INSTALL_NEW_APP");
               new Functions(MyMqttService.this, jsonObject);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            mqttClient.disconnect(0);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private class MyCallBack implements MqttCallback {
        private ContextWrapper context;
        public  String KEY_Type = "type";
        public  String KEY_GeneratedId = "generatedid";


        public MyCallBack(ContextWrapper context) {

            this.context = context;
        }


        @Override
        public void connectionLost(Throwable throwable) {
            Log.e(TAG, "mqtt connection lost");
            try {
                MyMqttService.mqttClient.connect(MyMqttService.mqttOptions);
            } catch (MqttException e) {
                // aTODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
            final JSONObject jsonObject = new JSONObject(new String( mqttMessage.getPayload()));
            Log.e(TAG, "mqtt message arrived"+jsonObject.toString());
            new Functions(MyMqttService.this, jsonObject);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
            Log.e(TAG, "mqtt delivery complete");
        }
    } */
}
