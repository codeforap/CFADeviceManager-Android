package com.cfap.cfadevicemanager;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;

/**
 * Created by Shreya Jagarlamudi on 03/09/15.
 */
public class LocationService extends IntentService {

    private String TAG = "LocationService";
    private DatabaseHelper myDbHelp;
    private LocationDetector locationDetector;
    GlobalState gs;

    private String KEY_IMEI = "imei";
    private String KEY_Battery = "battery";
    private String KEY_Model = "model";
    private String KEY_Version = "version";
    private String KEY_Type = "type";
    private String KEY_Status = "connStatus";
    private String KEY_Location = "location";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public LocationService() {
        super("LocationService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        gs = (GlobalState) getApplication();
        myDbHelp = DatabaseHelper.getInstance(this);
        locationDetector = new LocationDetector(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.e(TAG, "in onHandleIntent");
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a");
        ISTDateTime ist = new ISTDateTime();
        String connTime = formatter.format(ist.getIST());
        try {
            JSONObject json = new JSONObject();
            JSONArray jArray = new JSONArray();
            JSONArray batteryArray = new JSONArray();

            String battery_status = gs.getBatteryStatus();
            batteryArray.put(battery_status.substring(0, nthOccurrence(battery_status, '%', 0)));
            batteryArray.put(battery_status.substring(nthOccurrence(battery_status, '%', 0) + 1, nthOccurrence(battery_status, ' ', 2)));
            batteryArray.put(battery_status.substring(nthOccurrence(battery_status, ' ', 2) + 1, nthOccurrence(battery_status, ' ', 5)));
            batteryArray.put(battery_status.substring(nthOccurrence(battery_status, ' ', 5) + 1));
            json.put(KEY_Battery, batteryArray);
            Log.e(TAG, "creating  location json");
            String currLoc = locationDetector.getCurrLocation();
            String lastLocTime = locationDetector.getLastLocTime();
            jArray.put(currLoc.substring(0, nthOccurrence(currLoc, ',', 0)));
            jArray.put(currLoc.substring(nthOccurrence(currLoc, ',', 0) + 2));
            jArray.put(lastLocTime);
            json.put(KEY_Location, jArray);
            Log.e(TAG, "inserted location into json object");
            json.put(KEY_Type, "LocUpdate");
            json.put(KEY_IMEI, myDbHelp.getImei());
            json.put(KEY_Status, gs.getConnStatus() + " " + connTime);
            String jString = json.toString();
            myDbHelp.insertTask(connTime, "LocUpdate", jString, "pending");
            Log.e(TAG, "Location Update: " + jString);
            new FetchFromDatabase(this);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private static int nthOccurrence(String str, char c, int n) {
        int pos = str.indexOf(c, 0);
        while (n-- > 0 && pos != -1)
            pos = str.indexOf(c, pos+1);
        return pos;
    }
}
