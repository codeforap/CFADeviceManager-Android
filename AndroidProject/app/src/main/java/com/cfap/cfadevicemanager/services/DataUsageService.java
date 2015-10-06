package com.cfap.cfadevicemanager.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.cfap.cfadevicemanager.dbmodels.DataTrackerDBModel;
import com.cfap.cfadevicemanager.dbmodels.DatabaseHelper;
import com.cfap.cfadevicemanager.models.AppTrafficRecord;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by Shreya Jagarlamudi on 01/10/15.
 */
public class DataUsageService extends IntentService {

    private List<AppTrafficRecord> trafficRecords;
    private DatabaseHelper myDbHelp;

    private String TAG = "DataUsageService";
    private String KEY_IMEI = "imei";
    private String KEY_Type = "type";
    private String KEY_APP_NAME = "APP_NAME";
    private String KEY_WIFI_DATA = "WIFI_DATA";
    private String KEY_CELLULAR_DATA = "CELLULAR_DATA";
    private String DATA_USAGE_BY_APP = "DATA_USAGE_BY_APP";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public DataUsageService() {
        super("DataUsageService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        myDbHelp = DatabaseHelper.getInstance(this);
        trafficRecords = DataTrackerDBModel.getAppRecordsForToday(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a");
        ISTDateTime ist = new ISTDateTime();
        String connTime = formatter.format(ist.getIST());
        JSONObject json = new JSONObject();
        JSONArray jArray = new JSONArray();
        try {
            json.put(KEY_Type, "DataUsage");
            json.put(KEY_IMEI, myDbHelp.getImei());
            for(int i=0; i<trafficRecords.size(); i++) {
                AppTrafficRecord appTrafficRecord = trafficRecords.get(i);
                JSONObject subjson = new JSONObject();
                subjson.put(KEY_APP_NAME, appTrafficRecord.getName());
                subjson.put(KEY_WIFI_DATA, appTrafficRecord.getWifiData());
                subjson.put(KEY_CELLULAR_DATA, appTrafficRecord.getNetworkData());
                jArray.put(subjson);
            }
            json.put(DATA_USAGE_BY_APP, jArray);
            String jString = json.toString();
            Log.e(TAG, "DataUsage json: " + jString);
            myDbHelp.insertTask(connTime, "DataUsage", jString, "pending");
            new FetchFromDatabase(this, "myimei");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
