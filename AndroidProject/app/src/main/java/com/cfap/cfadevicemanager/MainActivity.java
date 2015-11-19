package com.cfap.cfadevicemanager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cfap.cfadevicemanager.dbmodels.DatabaseHelper;
import com.cfap.cfadevicemanager.services.AppTrackerService;
import com.cfap.cfadevicemanager.services.CFAReceiver;
import com.cfap.cfadevicemanager.services.GPSTracker;
import com.cfap.cfadevicemanager.services.ISTDateTime;
import com.cfap.cfadevicemanager.services.MyDeviceAdminReceiver;
import com.cfap.cfadevicemanager.services.MyMqttService;
import com.cfap.cfadevicemanager.services.SendToServer;
import com.cfap.cfadevicemanager.utils.Constants;
import com.cfap.cfadevicemanager.utils.Intents;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private String TAG = "MainActivity";
    GoogleApiClient googleApiClient;
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
    private String intendedTime = "11:59:00 PM";
    private GPSTracker gps;

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
    private String KEY_BLUETOOTH = "bluetoothStatus";
    private String KEY_GPS_STATUS = "gpsStatus";
    private String KEY_WIFI_TETHERING = "tetheringWifi";
    private String KEY_USB_TETHERING = "tetheringUsb";
    private String KEY_BLUETOOTH_TETHERING = "tetheringBluetooth";
    private String KEY_WIFI_STATUS = "wifiStatus";
    private String KEY_MOBILE_DATA_STATUS = "mobileDataStatus";

    private int LOC_INTERVAL = (60000)*20; //20 minutes
    private int APPUSAGE_INTERVAL = (60000)*60*24; //24 hours
    private int FOREGROUND_INTERVAL = 3000; // 3 seconds
    private int DATAUSAGE_INTERVAL = 30000; // 30 seconds
    private DatabaseHelper myDbHelp;
   // private LocationDetector locationDetector;
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
        gps = new GPSTracker(getApplicationContext());

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

        getBaseContext().getApplicationContext().sendBroadcast(
                new Intent("StartupReceiver_Manual_Start"));

        Battery_Status = gs.getBatteryStatus();
     //   locationDetector = new LocationDetector(this);

        myDbHelp = DatabaseHelper.getInstance(getApplicationContext());
        try {
            myDbHelp.createDataBase();
            imei = gs.getDeviceImei();
            myDbHelp.insertImei(imei);
        } catch (IOException e) {
            // TODO Auto-generated catch block;
            e.printStackTrace();
        }

        Intent serviceIntent = new Intent(this, MyMqttService.class);
        serviceIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startService(serviceIntent);

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
            maintv.setVisibility(View.VISIBLE);
            maintv.setText("Your device is now registered with the Government of Andhra Pradesh, India");
            if (googleApiClient == null) {
                googleApiClient = new GoogleApiClient.Builder(this)
                        .addApi(LocationServices.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this).build();
                googleApiClient.connect();
                LocationRequest locationRequest = LocationRequest.create();
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                locationRequest.setInterval(30 * 1000);
                locationRequest.setFastestInterval(5 * 1000);
                LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                        .addLocationRequest(locationRequest);

                //**************************
                builder.setAlwaysShow(true); //this is the key ingredient
                //**************************
                PendingResult<LocationSettingsResult> result =
                        LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
                result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                    @Override
                    public void onResult(LocationSettingsResult result) {
                        final Status status = result.getStatus();
                        final LocationSettingsStates state = result.getLocationSettingsStates();
                        switch (status.getStatusCode()) {
                            case LocationSettingsStatusCodes.SUCCESS:
                                // All location settings are satisfied. The client can initialize location
                                // requests here.
                                break;
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                // Location settings are not satisfied. But could be fixed by showing the user
                                // a dialog.
                                try {
                                    // Show the dialog by calling startResolutionForResult(),
                                    // and check the result in onActivityResult().
                                    status.startResolutionForResult(
                                            MainActivity.this, 1000);
                                } catch (IntentSender.SendIntentException e) {
                                    // Ignore the error.
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                // Location settings are not satisfied. However, we have no way to fix the
                                // settings so we won't show the dialog.
                                break;
                        }
                    }
                });
            }
          /*  Intent serviceIntent = new Intent(this, MyMqttService.class);
            serviceIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startService(serviceIntent); */
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
        if(myDbHelp.getRegistered(imei)==0) {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a");
            ISTDateTime ist = new ISTDateTime();
            String connTime = formatter.format(ist.getIST());

                JSONObject json = new JSONObject();
                JSONArray jArray = new JSONArray();
                JSONArray batteryArray = new JSONArray();
                try {
                    json.put(KEY_WIFI_TETHERING, wifiTetheringStatus());
                    json.put(KEY_BLUETOOTH, bluetoothStatus());
                    json.put(KEY_GPS_STATUS, gpsStatus());
                    batteryArray.put(Battery_Status.substring(0, nthOccurrence(Battery_Status, '%', 0)));
                    batteryArray.put(Battery_Status.substring(nthOccurrence(Battery_Status, '%', 0) + 1, nthOccurrence(Battery_Status, ' ', 2)));
                    batteryArray.put(Battery_Status.substring(nthOccurrence(Battery_Status, ' ', 2) + 1, nthOccurrence(Battery_Status, ' ', 5)));
                    batteryArray.put(Battery_Status.substring(nthOccurrence(Battery_Status, ' ', 5) + 1));
                    json.put(KEY_Battery, batteryArray);
                  /*  String currLoc = locationDetector.getCurrLocation();
                    String lastLoc = locationDetector.getLastLocTime();
                    jArray.put(currLoc.substring(0, nthOccurrence(currLoc, ',', 0)));
                    jArray.put(currLoc.substring(nthOccurrence(currLoc, ',', 0) + 2));
                    jArray.put(lastLoc);
                    json.put(KEY_Location, jArray);*/
                    double latitude = gps.getLatitude();
                    double longitude = gps.getLongitude();
                    if (latitude != 0 && longitude !=0) {
                        json.put(Constants.Device.MOBILE_DEVICE_LATITUDE, latitude);
                        json.put(Constants.Device.MOBILE_DEVICE_LONGITUDE, longitude);
                    }
                    json.put(KEY_Version, gs.getAndroidVersion());
                    json.put(KEY_Model, gs.getDeviceModel());
                    json.put(KEY_IMEI, gs.getDeviceImei());
                    json.put(KEY_Type, "Registration");
                    json.put(KEY_Status, gs.getConnStatus() + " " + connTime);
                    String jString = json.toString();
                    myDbHelp.insertTask(connTime, "Registration", jString, "pending");

               //     try {

                     //   sendMqtt = new SendToServer(MainActivity.this, json, "APGOV");
                    try {
                        MyMqttService.publishToServer(json, "APGOV");
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }

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


                       // invoked at 11:59 PM every night
                  /*      try {
                            String string1 = "23:59:00";
                            Date time1 = null;
                            time1 = new SimpleDateFormat("HH:mm:ss").parse(string1);
                            Calendar calendar1 = Calendar.getInstance();
                            calendar1.setTime(time1);
                            Intent App_intent = new Intent(MainActivity.this, CFAReceiver.class);
                            App_intent.putExtra("serviceType", "AppUsage");
                            PendingIntent App_PendingIntent = PendingIntent.getBroadcast(MainActivity.this, 2, App_intent, 0);
                            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar1.getTimeInMillis(), AlarmManager.INTERVAL_DAY, App_PendingIntent);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        } */


                        // Uncomment this if you only want your app and system installed apps to open. The user will not be able to open any toher apps
                     /*   Intent Foreground_intent = new Intent(MainActivity.this, CFAReceiver.class);
                        Foreground_intent.putExtra("serviceType", "Foreground");
                        PendingIntent Fore_PendingIntent = PendingIntent.getBroadcast(MainActivity.this, 2, Foreground_intent, 0);
                        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60000, FOREGROUND_INTERVAL, Fore_PendingIntent);
*/

                        if (googleApiClient == null) {
                            googleApiClient = new GoogleApiClient.Builder(this)
                                    .addApi(LocationServices.API)
                                    .addConnectionCallbacks(this)
                                    .addOnConnectionFailedListener(this).build();
                            googleApiClient.connect();
                            LocationRequest locationRequest = LocationRequest.create();
                            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                            locationRequest.setInterval(30 * 1000);
                            locationRequest.setFastestInterval(5 * 1000);
                            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                                    .addLocationRequest(locationRequest);

                            //**************************
                            builder.setAlwaysShow(true); //this is the key ingredient
                            //**************************
                            PendingResult<LocationSettingsResult> result =
                                    LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
                            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                                @Override
                                public void onResult(LocationSettingsResult result) {
                                    final Status status = result.getStatus();
                                    final LocationSettingsStates state = result.getLocationSettingsStates();
                                    switch (status.getStatusCode()) {
                                        case LocationSettingsStatusCodes.SUCCESS:
                                            // All location settings are satisfied. The client can initialize location
                                            // requests here.
                                            break;
                                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                            // Location settings are not satisfied. But could be fixed by showing the user
                                            // a dialog.
                                            try {
                                                // Show the dialog by calling startResolutionForResult(),
                                                // and check the result in onActivityResult().
                                                status.startResolutionForResult(
                                                        MainActivity.this, 1000);
                                            } catch (IntentSender.SendIntentException e) {
                                                // Ignore the error.
                                            }
                                            break;
                                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                            // Location settings are not satisfied. However, we have no way to fix the
                                            // settings so we won't show the dialog.
                                            break;
                                    }
                                }
                            });
                        }

                   /* } catch (MqttException e) {
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

                }*/

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
    }}

    public static int nthOccurrence(String str, char c, int n) {
        int pos = str.indexOf(c, 0);
        while (n-- > 0 && pos != -1) {
            pos = str.indexOf(c, pos + 1);
        }
        return pos;
    }

    public String bluetoothStatus(){
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            return "notsupported";
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                // Bluetooth is not enable :)
                return "disabled";
            }else{
                return "enabled";
            }
        }
    }

    public String gpsStatus(){
        PackageManager packMan = getPackageManager();
        if(packMan.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)==true){
            LocationManager manager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            if(manager.isProviderEnabled(LocationManager.GPS_PROVIDER)==true){
                return "enabled";
            }else{
                return "disabled";
            }
        }else{
            return "notSupported";
        }
    }

    public boolean wifiTetheringStatus() {
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        Method[] wmMethods = wifi.getClass().getDeclaredMethods();
        for (Method method : wmMethods) {
            if (method.getName().equals("isWifiApEnabled")) {
                try {
                    boolean isWifiAPenabled = (boolean) method.invoke(wifi);
                    return isWifiAPenabled;
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent i = new Intent(this, AppTrackerService.class);
        i.setAction("APP_DATA_REFRESH");
        startService(i);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
