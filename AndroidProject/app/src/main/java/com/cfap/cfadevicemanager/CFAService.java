package com.cfap.cfadevicemanager;

/**
 * Created by Shreya Jagarlamudi on 27/07/15.
 */

import android.app.AlarmManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;


/**
 * This class does all the work in the background even when the app is killed: connecting to the server, updating data to server etc.
 * Here is what is happening:
 * When this service is first created on launch of mainactivity, we set up location and all the other details and start sendtask
 * from then on, everytime the location is changed, onLocationChanged gets triggered and we execute sendtask again
 * As of now, we do not hear for any queries from server. We only publish to server. Next version will deal with subscribing
 * and taking requests from server.
 */
public class CFAService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private String TAG = "CFAService";
    private String KEY_IMEI = "imei";
    private String KEY_Battery = "battery";
    private String KEY_Model = "model";
    private String KEY_Version = "version";
    private String KEY_Location = "location";
  //  private String KEY_LocTime = "lastloctime";
    private String KEY_Status = "connStatus";
    private String KEY_Type = "type";
    private String KEY_Applist = "installed_apps";
    private String Server = "yourserveraddress";
    public static final String BROADCAST_ACTION = "com.cfap.cfadevicemanager.CFAService";
    GlobalState gs;
    private final Handler handler = new Handler();
    private final Handler appHandler = new Handler();
    private final Handler dbHandler = new Handler();
    Intent intent;
    int counter = 0;
    NetworkStateReceiver mConnReceiver;

    private MqttClient mqttClient;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    SendTask sendAll;
    RegularTask sendData;
    AppTask sendAppData;
    DataUsageToDb updateDataDb;
    TrafficSnapshot latest=null;
    TrafficSnapshot previous=null;

    TrafficSnapshot latestForDb;
    TrafficSnapshot previousForDb;

    private long UPDATE_INTERVAL = 200000; // updates location every 20 mins
    private long FASTEST_INTERVAL = 150000;
    private String currLocation; // string version of curr location or last known location
    private String lastLocTime; // time of curr location or last known location
 //   private String connStatus="unknown"; // used to store if device is online or offline
    private String statusTime; //used to store the time at which device is online or offline
    private String jsonStr;
    private String clientID;

    private DatabaseHelper mydbHelp;

    @Override
    public void onCreate() {
        Log.e(TAG, "in onCreate");
        super.onCreate();
        gs = (GlobalState) getApplication();
        intent = new Intent(BROADCAST_ACTION);

        mydbHelp = DatabaseHelper.getInstance(getApplicationContext());
        try {
            mydbHelp.createDataBase();
        } catch (IOException e) {
            // TODO Auto-generated catch block;
            e.printStackTrace();
        }

      //  Log.e(TAG, "device imei from sqlite db: "+mydbHelp.getImei());

        mConnReceiver = new NetworkStateReceiver();
        registerReceivers();
        buildGoogleApiClient();
        if(mGoogleApiClient!= null){
            mGoogleApiClient.connect();
        }else{
            Log.e(TAG, "not connected");
        }
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        String imei = getDeviceImei();
        Log.e(TAG, "reg val: "+mydbHelp.getRegistered(imei));
        if(mydbHelp.getRegistered(imei)==0){
            sendAll = new SendTask();
            mydbHelp.insertImei(imei);
            mydbHelp.insertRegistered(1, getDeviceImei());
           // Log.e(TAG, "reg val1: " + mydbHelp.getRegistered(imei));
           // mydbHelp.writeAppsListToDb(getInstalledApps(), today);
            sendAll.execute();
        }
        handler.removeCallbacks(executeTask);
        handler.postDelayed(executeTask, (60000)*20); // 20 minutes
        appHandler.removeCallbacks(appUpdateTask);
        appHandler.postDelayed(appUpdateTask, (60000) * 60 * 24); // 24 hours
        dbHandler.removeCallbacks(dbUpdatedataTask);
        dbHandler.postDelayed(dbUpdatedataTask, 30000); // 30 seconds
    }

    private void registerReceivers() {
        registerReceiver(mConnReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "in onStartCommand");
        gs = (GlobalState) getApplication();
        return START_STICKY;
    }

    private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {
            DisplayLoggingInfo();
           // handler.postDelayed(this, 5000); // 5 seconds
        }
    };

    /**
     * Defining runnable to execute our location update (RegularTask) every 20 minutes
     */
    private Runnable executeTask;

    {
        executeTask = new Runnable() {
            @Override
            public void run() {
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                        mGoogleApiClient);
                if (mLastLocation != null) {
                    setCurrLocation(String.valueOf(mLastLocation.getLatitude()) + ", " + String.valueOf(mLastLocation.getLongitude()));
                    Long time = mLastLocation.getTime();
                    setLastLocTime(time);
                    Log.e(TAG, currLocation + " " + getLastLocTime());
                }
                sendData = new RegularTask();
                sendData.execute();
                handler.postDelayed(this, (60000)*20); // 20 minutes
            }
        };
    }

    /**
     * defining runnable to execute AppTask every 24 hours. Sends the apps installed list with version,
     * install date & data usage since boot
     */
    private Runnable appUpdateTask = new Runnable() {
        @Override
        public void run() {
            sendAppData = new AppTask();
            sendAppData.execute();
            appHandler.postDelayed(this, (60000)*60*24); //24 hours
        }
    };

    /**
     * defining runnable to execute DataUsageToDb task every 30 seconds.
     */
    private Runnable dbUpdatedataTask = new Runnable() {
        @Override
        public void run() {
            updateDataDb = new DataUsageToDb();
            updateDataDb.execute();
            dbHandler.postDelayed(this, 30000);
        }
    };

    /**
     *
     */
    private void DisplayLoggingInfo() {
        Log.e(TAG, "entered DisplayLoggingInfo");

        intent.putExtra("jstring", jsonStr);
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "in onBind!");
        return null;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        registerReceivers();
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "in onDestroy");
        if(mConnReceiver!=null)  unregisterReceiver(mConnReceiver);
        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.e(TAG, "in onConnected");
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            setCurrLocation(String.valueOf(mLastLocation.getLatitude()) + ", " + String.valueOf(mLastLocation.getLongitude()));
            Long time = mLastLocation.getTime();
            setLastLocTime(time);
            Log.e(TAG, currLocation+" "+getLastLocTime());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "in onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "in onConnectionFailed");
    }



    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if (mLastLocation != null) {
           setCurrLocation(String.valueOf(mLastLocation.getLatitude())+", "+String.valueOf(mLastLocation.getLongitude()));
            setLastLocTime(mLastLocation.getTime());
            Log.e(TAG, "currLocation changed: "+currLocation+" "+getLastLocTime());
        }
        sendData = new RegularTask();
        sendData.execute();
    }

    private void setCurrLocation(String loc){
        currLocation = loc;
    }
    private String getCurrLocation(){
        return currLocation;
    }

    private void setLastLocTime(long t){

       // SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
       // lastLocTime = formatter.format(t);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a");
        Date d = new Date(t);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+05:30"));
        lastLocTime = sdf.format(d);
    }
    private String getLastLocTime(){
        return lastLocTime;
    }

    public String getDeviceModel(){
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    public String getAndroidVersion() {
        String release = Build.VERSION.RELEASE;
        int sdkVersion = Build.VERSION.SDK_INT;
        return "Android SDK: " + sdkVersion + " (" + release +")";
    }

    public String getDeviceImei(){
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId().toString();
    }

    public String getBatteryStatus(){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);

        // are we charging?
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        // How are we charging?
        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = (level / (float)scale)*100;
        return String.valueOf(batteryPct)+"% charging?: "+isCharging+" AC Charging?: "+acCharge+" USB Charging?: "+usbCharge;
    }

    /**
     * writes list of installed apps with date to the db. Not using this method anywhere as of now
     */
    public void writeInstalledAppsToDb(){
        ArrayList appArray = new ArrayList();
        final PackageManager pm = getPackageManager();
        //get a list of installed apps.
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo packageInfo : packages) {
            appArray.add(packageInfo.loadLabel(pm).toString());
            String appname = packageInfo.loadLabel(pm).toString();
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            String today = sdf.format(getCurrIndianDate());
            mydbHelp.appEntry(appname, today);
        }
       // Log.e(TAG, "InstalledApps: "+appArray);
       // return appArray;
    }

    /**
     * Builds Json Object to be sent to the server with the apps list, install date, version and also
     * bytes of data sent and received for each app.
     * @return JsonArray
     */
    public JSONArray getAppJson(){
        JSONArray jarray = new JSONArray();
        final PackageManager pm = getPackageManager();
        //get a list of installed apps.
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {
         //   Log.e(TAG, "APP NAME: "+packageInfo.loadLabel(pm));
            JSONObject subObj = new JSONObject();

            try {
                PackageInfo pkgInfo = pm.getPackageInfo(packageInfo.packageName, 0);
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                String today = sdf.format(getCurrIndianDate());
            //    subObj.put("app_daily_bytes_rec", mydbHelp.getDailyUsageRec(packageInfo.loadLabel(pm).toString(), today));
             //   subObj.put("app_daily_bytes_sent", mydbHelp.getDailyUsageSent(packageInfo.loadLabel(pm).toString(), today));
                subObj.put("app_installdate", pkgInfo.firstInstallTime);
             //   subObj.put("app_size", packageInfo.);
                subObj.put("app_version", pkgInfo.versionName);
                subObj.put("app_name", packageInfo.loadLabel(pm).toString());

                int uid = packageInfo.uid;
                //adding data sent and received for each app
                previous=latest;
                latest=new TrafficSnapshot(this);
                ArrayList<String> log=new ArrayList<String>();
                TrafficRecord latest_rec=latest.apps.get(uid);
                TrafficRecord previous_rec=
                        (previous==null ? null : previous.apps.get(uid));

                emitLog(latest_rec.tag, latest_rec, previous_rec, log);
                Collections.sort(log);

                for (String row : log) {
                //    Log.e("CFA TrafficMonitor", row);
                    subObj.put("app_data_usage_bytes", row);
                }

                jarray.put(subObj);


            } catch (JSONException e) {
                e.printStackTrace();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
      /*  //adding data sent and received for each app
        previous=latest;
        latest=new TrafficSnapshot(this);
        ArrayList<String> log=new ArrayList<String>();
        HashSet<Integer> intersection=new HashSet<Integer>(latest.apps.keySet());
        if (previous!=null) {
            intersection.retainAll(previous.apps.keySet());
        }

        for (Integer uid : intersection) {
            TrafficRecord latest_rec=latest.apps.get(uid);
            TrafficRecord previous_rec=
                    (previous==null ? null : previous.apps.get(uid));

            emitLog(latest_rec.tag, latest_rec, previous_rec, log);
        }

        Collections.sort(log);

        for (String row : log) {
            Log.e("CFA TrafficMonitor", row);
        } */
        return jarray;
    }

    /**
     * Builds a string with data received and sent for each app and puts all strings into an arraylist to return
     * @param name
     * @param latest_rec
     * @param previous_rec
     * @param rows
     */
    private void emitLog(CharSequence name, TrafficRecord latest_rec,
                         TrafficRecord previous_rec,
                         ArrayList<String> rows) {
        if (latest_rec.rx>-1 || latest_rec.tx>-1) {
            StringBuilder buf=new StringBuilder(name);

            buf.append("=");
            buf.append(String.valueOf(latest_rec.rx));
            buf.append(" received");

            if (previous_rec!=null) {
                buf.append(" (delta=");
                buf.append(String.valueOf(latest_rec.rx-previous_rec.rx));
                buf.append(")");
            }

            buf.append(", ");
            buf.append(String.valueOf(latest_rec.tx));
            buf.append(" sent");

            if (previous_rec!=null) {
                buf.append(" (delta=");
                buf.append(String.valueOf(latest_rec.tx-previous_rec.tx));
                buf.append(")");
            }

            rows.add(buf.toString());
        }
    }

    /** We run all the connections code in an async task so that we do not block the main/UI thread. everything runs
     *on a separate background thread here. We connect to the server using mqttclient and publish complete data in a json format.
     * This task runs when we first create the service. Only once!
     * */
    private class SendTask extends AsyncTask<String, String, String>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(String... params) {
            String connTime = "";
            try {
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a");
              //  TimeZone.setDefault(TimeZone.getTimeZone("UTC+5:30"));
              //  formatter.setTimeZone(TimeZone.getDefault());
              //  connTime = formatter.format(new Date());
                connTime = formatter.format(getCurrIndianDate());
                clientID = getDeviceImei()+" "+connTime;
                mqttClient = new MqttClient(Server, clientID, new MemoryPersistence());
                Log.e(TAG, "sendTask clientID: "+clientID);
                mqttClient.connect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
            JSONObject json = new JSONObject();
            JSONArray jArray = new JSONArray();
            JSONArray batteryArray = new JSONArray();
            try {
                json.put(KEY_Applist, getAppJson());
                json.put(KEY_Type, "Registration");
                json.put(KEY_IMEI, getDeviceImei());
                json.put(KEY_Model, getDeviceModel());
                json.put(KEY_Version, getAndroidVersion());
                Log.e(TAG, "sendTask currLoc: "+getCurrLocation());
                jArray.put(getCurrLocation().substring(0, nthOccurrence(getCurrLocation(), ',', 0)));
                jArray.put(getCurrLocation().substring(nthOccurrence(getCurrLocation(), ',', 0) + 2));
                jArray.put(getLastLocTime());
                json.put(KEY_Location, jArray);
          //      json.put(KEY_LocTime, getLastLocTime());
              //  json.put(KEY_Battery, getBatteryStatus());
                batteryArray.put(getBatteryStatus().substring(0, nthOccurrence(getBatteryStatus(), '%', 0)));
                batteryArray.put(getBatteryStatus().substring(nthOccurrence(getBatteryStatus(), '%', 0) + 1, nthOccurrence(getBatteryStatus(), ' ', 2)));
                batteryArray.put(getBatteryStatus().substring(nthOccurrence(getBatteryStatus(), ' ', 2)+1, nthOccurrence(getBatteryStatus(), ' ', 5)));
                batteryArray.put(getBatteryStatus().substring(nthOccurrence(getBatteryStatus(), ' ', 5)+1));
                json.put(KEY_Battery, batteryArray);
                json.put(KEY_Status, gs.getConnStatus()+" "+connTime);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            String jString = json.toString();
            try {
                final MqttMessage message = new MqttMessage(jString.getBytes());
                final byte[] b = message.getPayload();
                mqttClient.publish("Details", b, 2, false);
            } catch (MqttException e) {
                e.printStackTrace();
            }
            Log.e(TAG, "sendTask: locationStr: "+getCurrLocation());
            Log.e(TAG, "sendTask: json String: "+jString);
            jsonStr = jString;
            gs.setJStr(jString);
            return jString;
        }

        @Override
        protected void onPostExecute(String s) {
            // handler.removeCallbacks(sendUpdatesToUI);
            handler.post(sendUpdatesToUI);
        }
    }

    /** We run all the connections code in an async task so that we do not block the main/UI thread. everything runs
     *on a separate background thread here. We connect to the server using mqttclient and publish locatin & status data in a json format.
     * This task runs every 20 mins or when we change the location.
     * */
    private class RegularTask extends AsyncTask<String, String, String>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(String... params) {
            String connTime = "";
            try {
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a");
               // TimeZone.setDefault(TimeZone.getTimeZone("UTC+5:30"));
              //  formatter.setTimeZone(TimeZone.getDefault());
              //  connTime = formatter.format(new Date());
                connTime = formatter.format(getCurrIndianDate());
                clientID = getDeviceImei()+" "+connTime;
                mqttClient = new MqttClient(Server, clientID, new MemoryPersistence());
                Log.e(TAG, "RegularTask clientID: "+clientID);
                mqttClient.connect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
            JSONObject json = new JSONObject();
            JSONArray jArray = new JSONArray();
            JSONArray batteryArray = new JSONArray();
            try {
                json.put(KEY_Type, "LocUpdate");
                json.put(KEY_IMEI, getDeviceImei());
                jArray.put(getCurrLocation().substring(0, nthOccurrence(getCurrLocation(), ',', 0)));
                jArray.put(getCurrLocation().substring(nthOccurrence(getCurrLocation(), ',', 0) + 2));
                jArray.put(getLastLocTime());
                json.put(KEY_Location, jArray);

                batteryArray.put(getBatteryStatus().substring(0, nthOccurrence(getBatteryStatus(), '%', 0)));
                batteryArray.put(getBatteryStatus().substring(nthOccurrence(getBatteryStatus(), '%', 0) + 1, nthOccurrence(getBatteryStatus(), ' ', 2)));
                batteryArray.put(getBatteryStatus().substring(nthOccurrence(getBatteryStatus(), ' ', 2)+1, nthOccurrence(getBatteryStatus(), ' ', 5)));
                batteryArray.put(getBatteryStatus().substring(nthOccurrence(getBatteryStatus(), ' ', 5)+1));
                json.put(KEY_Battery, batteryArray);
                json.put(KEY_Status, gs.getConnStatus()+" "+connTime);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String jString = json.toString();
            try {
                final MqttMessage message = new MqttMessage(jString.getBytes());
                final byte[] b = message.getPayload();
                mqttClient.publish("Details", b, 2, false);
             //   mqttClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
            Log.e(TAG, "RegularTask: locationStr: "+getCurrLocation());
            Log.e(TAG, "RegularTask: json String: "+jString);
            jsonStr = jString;
            gs.setJStr(jString);
            return jString;
        }

    }

    /** We run all the connections code in an async task so that we do not block the main/UI thread. everything runs
     *on a separate background thread here. We connect to the server using mqttclient and publish locatin & status data in a json format.
     * This task runs every 24 hours to update the list of apps installed on device and the data usage.
     * */
    private class AppTask extends AsyncTask<String, String, String>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(String... params) {
            String connTime = "";
            try {
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a");
                // TimeZone.setDefault(TimeZone.getTimeZone("UTC+5:30"));
                //  formatter.setTimeZone(TimeZone.getDefault());
                //  connTime = formatter.format(new Date());
                connTime = formatter.format(getCurrIndianDate());
                clientID = getDeviceImei()+" "+connTime;
                mqttClient = new MqttClient(Server, clientID, new MemoryPersistence());
                Log.e(TAG, "AppTask clientID: "+clientID);
                mqttClient.connect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
            JSONObject json = new JSONObject();

            try {
                json.put(KEY_Type, "AppsUpdate");
                json.put(KEY_IMEI, getDeviceImei());
                json.put(KEY_Applist, getAppJson());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String jString = json.toString();
            try {
                final MqttMessage message = new MqttMessage(jString.getBytes());
                final byte[] b = message.getPayload();
                mqttClient.publish("Details", b, 2, false);
            } catch (MqttException e) {
                e.printStackTrace();
            }
            Log.e(TAG, "AppTask: locationStr: "+getCurrLocation());
            Log.e(TAG, "AppTask: json String: "+jString);
            jsonStr = jString;
            gs.setJStr(jString);
            return jString;
        }

        @Override
        protected void onPostExecute(String s) {
            // handler.removeCallbacks(sendUpdatesToUI);
            handler.post(sendUpdatesToUI);
        }
    }

    /**
     * Not using this as of now. Written to update data usage to local database
     */
    private class DataUsageToDb extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... params) {
           /* final PackageManager pm = getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo packageInfo : packages) {
                int uid = packageInfo.uid;
                previousForDb = latestForDb;
                latestForDb=new TrafficSnapshot(CFAService.this);
                ArrayList<String> log=new ArrayList<String>();
                TrafficRecord latest_rec=latestForDb.apps.get(uid);
                TrafficRecord previous_rec=
                        (previousForDb==null ? null : previousForDb.apps.get(uid));
                emitLog(latest_rec.tag, latest_rec, previous_rec, log);
                Collections.sort(log);
                for (String row : log) {
                    String appLabel = packageInfo.loadLabel(pm).toString();
                    //   Log.e(TAG, "row: label: "+appLabel+" data usage: "+row);
                    long dataRecSinceBoot = latest_rec.rx;
                    long dataSentSinceBoot = latest_rec.tx;
                    Log.e(TAG, "noting app data to db: label: "+appLabel+" data usage: rec: "+dataRecSinceBoot+" sent: "+dataSentSinceBoot);
                    // Need to get day wise data usage for each app and put into db in order to calculate monthly and 45 days data usage. use alarm manager?
                    // keep pushing data usage to db every few mins and adding up for the day. At 11:00am every morning, push that previous day's data to the server

                    // or we could check the bytes sent and rec at start of the day , say 12 am and then end of the day, say 11:59 pm and endofday-startofday=bytesthatday

                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    String today = sdf.format(getCurrIndianDate());
                    Log.e(TAG, "today: "+today);
                    if(mydbHelp.checkforDupEntry(appLabel, today)==false){
                        mydbHelp.appEntry(appLabel, today);
                        mydbHelp.updateDailyUsageRec(appLabel, today, dataRecSinceBoot);
                        mydbHelp.updateDailyUsageSent(appLabel, today, dataSentSinceBoot);
                    } else{
                        // add up the current data usage to the existing usage

                    }
                }
            } */
            Log.e(TAG, "updateToDb task background");
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //super.onPostExecute(aVoid);
        }
    }

    public static int nthOccurrence(String str, char c, int n) {
        int pos = str.indexOf(c, 0);
        while (n-- > 0 && pos != -1)
            pos = str.indexOf(c, pos+1);
        return pos;
    }

    /**
     * Gets the current indian date no matter what the device time or time zone is set to
     * @return
     */
    public Date getCurrIndianDate() {
        // TODO Auto-generated method stub
        Date indianDate = null;

        SimpleDateFormat currformatter = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a");
        GregorianCalendar cal = new GregorianCalendar();
        String dstring = currformatter.format(cal.getTime());
       // Log.e("Tel Frag", "gettime in indian date: "+cal.getTime());
        Date rdate = null;
        try {
            rdate = currformatter.parse(dstring);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        TimeZone tz = TimeZone.getDefault();
        // From TimeZone current
        Log.i("Tel Frag", "getindiandate: TimeZone : " + tz.getID() + " - " + tz.getDisplayName());
        Log.i("Tel Frag", "getindiandate: TimeZone : " + tz);
        Log.i("Tel Frag", "getindiandate: Date : " + currformatter.format(rdate));

        // To TimeZone Asia/Calcutta
        SimpleDateFormat sdfIndia = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a");
        TimeZone tzInIndia = TimeZone.getTimeZone("Asia/Calcutta");
        sdfIndia.setTimeZone(tzInIndia);

        String sDateInIndia = sdfIndia.format(rdate); // Convert to String first
        Date dateInIndia = null;
        try {
            dateInIndia = currformatter.parse(sDateInIndia);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Log.i("Tel Frag", "getindiandate: \nTimeZone : " + tzInIndia.getID() +
                " - " + tzInIndia.getDisplayName());
        Log.i("Tel Frag", "getindiandate: TimeZone : " + tzInIndia);
        Log.i("Tel Frag", "getindiandate: Date (String) : " + sDateInIndia);
        Log.i("Tel Frag", "getindiandate: Date (Object) : " + currformatter.format(dateInIndia));

        return dateInIndia;
    }

}
