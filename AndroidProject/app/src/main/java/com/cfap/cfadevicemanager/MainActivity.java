package com.cfap.cfadevicemanager;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;

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
   // private Intent intent;
    private ImageView logoview;
    private TextView nameview;
    private TextView maintv;
    private EditText usernameet;
    private EditText passwordet;
    private Button submit;
    private SharedPreferences sharedpreferences;
    GlobalState gs;
    DevicePolicyManager devicePolicyManager;
    ComponentName demoDeviceAdmin;

    private final String MyPREFERENCES = "CfaPrefs" ;
    private final String usernamePref = "cfausername";
    private final String passwordPref = "cfapassword";

    private String KEY_IMEI = "imei";
    private String KEY_Battery = "battery";
    private String KEY_Model = "model";
    private String KEY_Version = "version";
    private String KEY_Type = "type";
    private String KEY_Status = "connStatus";
    private String KEY_Location = "location";

    private int LOC_INTERVAL = (60000)*20; //20 minutes
    private int APPUSAGE_INTERVAL = (60000)*60*24; //24 hours
    private DatabaseHelper myDbHelp;
    private LocationDetector locationDetector;
    private String imei = "";
    private String Battery_Status = "";
    private SendToServer sendMqtt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
      //  intent = new Intent(this, CFAService.class);
        gs = (GlobalState) getApplication();
        // Initialize Device Policy Manager service and our receiver class
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        demoDeviceAdmin = new ComponentName(this, MyDeviceAdminReceiver.class);

        if (devicePolicyManager.isAdminActive(demoDeviceAdmin)) {
            // do nothing
        }
        else {
            Intent intent=
                    new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, demoDeviceAdmin);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.device_admin_explanation));
            startActivity(intent);
        }

        Battery_Status = gs.getBatteryStatus();
        locationDetector = new LocationDetector(this);

        myDbHelp = DatabaseHelper.getInstance(getApplicationContext());
        try {
            myDbHelp.createDataBase();
        } catch (IOException e) {
            // TODO Auto-generated catch block;
            e.printStackTrace();
        }

        logoview = (ImageView) findViewById(R.id.logoview);
        nameview = (TextView) findViewById(R.id.nameview);
        usernameet = (EditText) findViewById(R.id.usernameet);
        passwordet = (EditText) findViewById(R.id.pwet);
        submit = (Button) findViewById(R.id.submit);
        maintv = (TextView) findViewById(R.id.maintv);

        maintv.setTextSize(20);

        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        Log.e(TAG, "saved username: "+sharedpreferences.getString(usernamePref, "noValun"));
        Log.e(TAG, "saved pw: "+sharedpreferences.getString(passwordPref, "noValpw"));

        if(sharedpreferences.getString(usernamePref, "noValun").equals("admin") &&
                sharedpreferences.getString(passwordPref, "noValpw").equals("cfap")){
            Log.e(TAG, "username & pw already saved");
            logoview.setVisibility(View.GONE);
            nameview.setVisibility(View.GONE);
            usernameet.setVisibility(View.GONE);
            passwordet.setVisibility(View.GONE);
            submit.setVisibility(View.GONE);


        }

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if((usernameet.getText().toString().equals("admin") && passwordet.getText().toString().equals("cfap"))
                        ){

                    logoview.setVisibility(View.GONE);
                    nameview.setVisibility(View.GONE);
                    usernameet.setVisibility(View.GONE);
                    passwordet.setVisibility(View.GONE);
                    submit.setVisibility(View.GONE);

                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString(usernamePref, "admin");
                    editor.putString(passwordPref, "cfap");
                    editor.commit();

                    maintv.setVisibility(View.VISIBLE);
                    maintv.setText("Registering Device...");

                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            register();
                        }
                    });
                    t.start();

                } else{
                    usernameet.setText("");
                    passwordet.setText("");
                    Toast.makeText(MainActivity.this, "The login details you entered are incorrect. Please try again!", 3).show();
                }
            }
        });
    }

    private void register(){
        imei = gs.getDeviceImei();
        myDbHelp.insertImei(imei);
        if(myDbHelp.getRegistered(imei)==0) {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a");
            ISTDateTime ist = new ISTDateTime();
            String connTime = formatter.format(ist.getIST());

                JSONObject json = new JSONObject();
                JSONArray jArray = new JSONArray();
                JSONArray batteryArray = new JSONArray();
                try {
                    batteryArray.put(Battery_Status.substring(0, nthOccurrence(Battery_Status, '%', 0)));
                    batteryArray.put(Battery_Status.substring(nthOccurrence(Battery_Status, '%', 0) + 1, nthOccurrence(Battery_Status, ' ', 2)));
                    batteryArray.put(Battery_Status.substring(nthOccurrence(Battery_Status, ' ', 2) + 1, nthOccurrence(Battery_Status, ' ', 5)));
                    batteryArray.put(Battery_Status.substring(nthOccurrence(Battery_Status, ' ', 5) + 1));
                    json.put(KEY_Battery, batteryArray);
                    String currLoc = locationDetector.getCurrLocation();
                    String lastLoc = locationDetector.getLastLocTime();
                    jArray.put(currLoc.substring(0, nthOccurrence(currLoc, ',', 0)));
                    jArray.put(currLoc.substring(nthOccurrence(currLoc, ',', 0) + 2));
                    jArray.put(lastLoc);
                    json.put(KEY_Location, jArray);
                    json.put(KEY_Version, gs.getAndroidVersion());
                    json.put(KEY_Model, gs.getDeviceModel());
                    json.put(KEY_IMEI, gs.getDeviceImei());
                    json.put(KEY_Type, "Registration");
                    json.put(KEY_Status, gs.getConnStatus() + " " + connTime);
                    String jString = json.toString();
                    myDbHelp.insertTask(connTime, "Registration", jString, "pending");

                    try {
                        sendMqtt = new SendToServer(MainActivity.this, json);
                        myDbHelp.insertRegistered(1, imei);
                        myDbHelp.updateTaskStatus(jString, "sent");
                        Log.e(TAG, "Registration json: " + jString);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                maintv.setText("Your device is now registered with the Government of Andhra Pradesh, India");
                            }
                        });

                        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                        Intent Loc_intent = new Intent(MainActivity.this, CFAReceiver.class);
                        Loc_intent.putExtra("serviceType", "Location");
                        PendingIntent Loc_PendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, Loc_intent, 0);
                        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), LOC_INTERVAL, Loc_PendingIntent);

                        Intent App_intent = new Intent(MainActivity.this, CFAReceiver.class);
                        App_intent.putExtra("serviceType", "AppUsage");
                        PendingIntent App_PendingIntent = PendingIntent.getBroadcast(MainActivity.this, 1, App_intent, 0);
                        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), APPUSAGE_INTERVAL, App_PendingIntent);


                    } catch (MqttException e) {
                        myDbHelp.insertRegistered(0, imei);
                        Log.e(TAG, "MQTT EXCEPTION");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                maintv.setText("We were not able to register your device. Please try again later!");
                                logoview.setVisibility(View.VISIBLE);
                                nameview.setVisibility(View.VISIBLE);
                                usernameet.setVisibility(View.VISIBLE);
                                passwordet.setVisibility(View.VISIBLE);
                                submit.setVisibility(View.VISIBLE);
                                maintv.setVisibility(View.GONE);
                            }
                        });
                        e.printStackTrace();
                    }


                } catch (JSONException e) {
                    myDbHelp.insertRegistered(0, imei);
                    Log.e(TAG, "JSON EXCEPTION");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            maintv.setText("We were not able to register your device. Please try again later!");
                            logoview.setVisibility(View.VISIBLE);
                            nameview.setVisibility(View.VISIBLE);
                            usernameet.setVisibility(View.VISIBLE);
                            passwordet.setVisibility(View.VISIBLE);
                            submit.setVisibility(View.VISIBLE);
                            maintv.setVisibility(View.GONE);
                        }
                    });
                    e.printStackTrace();
                }

            }
    }

    public static int nthOccurrence(String str, char c, int n) {
        int pos = str.indexOf(c, 0);
        while (n-- > 0 && pos != -1)
            pos = str.indexOf(c, pos+1);
        return pos;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}
