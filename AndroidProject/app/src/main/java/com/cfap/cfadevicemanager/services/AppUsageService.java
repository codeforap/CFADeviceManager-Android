package com.cfap.cfadevicemanager.services;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by Shreya Jagarlamudi on 03/09/15.
 */
public class AppUsageService extends IntentService{

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public AppUsageService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

//    private String TAG = "AppUsageService";
//    private DatabaseHelper myDbHelp;
//    GlobalState gs;
//    ISTDateTime ist;
//    TrafficSnapshot latest=null;
//    TrafficSnapshot previous=null;
//
//    private String KEY_IMEI = "imei";
//    private String KEY_Type = "type";
//    private String KEY_Applist = "installed_apps";
//    private String KEY_Status = "connStatus";
//
//    /**
//     * Creates an IntentService.  Invoked by your subclass's constructor.
//     */
//    public AppUsageService() {
//        super("AppUsageService");
//    }
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        gs = (GlobalState) getApplication();
//        myDbHelp = DatabaseHelper.getInstance(this);
//        ist = new ISTDateTime();
//    }
//
//    @Override
//    protected void onHandleIntent(Intent intent) {
//        Log.e(TAG, "in AppUsageService");
//        JSONObject json = new JSONObject();
//        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a");
//        String connTime = formatter.format(ist.getIST());
//        try {
//            json.put(KEY_Applist, getAppJson());
//            json.put(KEY_Type, "AppsUpdate");
//            json.put(KEY_IMEI, myDbHelp.getImei());
//            json.put(KEY_Status, gs.getConnStatus() + " " + connTime);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        String jString = json.toString();
//        myDbHelp.insertTask(connTime, "LocUpdate", jString, "pending");
//       // Log.e(TAG, "AppsUpdate: " + jString);
//        new FetchFromDatabase(this, "Details");
//       // Log.e(TAG, "Sent Jsons before erase: " + myDbHelp.getSentJsons());
//        myDbHelp.eraseSentDataFromDb();
//      //  Log.e(TAG, "Sent Jsons after erase: " + myDbHelp.getSentJsons());
//    }
//
//    /**
//     * Builds Json Object to be sent to the server with the apps list, install date, version and also
//     * bytes of data sent and received since boot for each app.
//     * @return JsonArray
//     */
//    public JSONArray getAppJson(){
//        JSONArray jarray = new JSONArray();
//        final PackageManager pm = getPackageManager();
//        //get a list of installed apps.
//        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
//
//        for (ApplicationInfo packageInfo : packages) {
//            //   Log.e(TAG, "APP NAME: "+packageInfo.loadLabel(pm));
//            JSONObject subObj = new JSONObject();
//
//            try {
//                PackageInfo pkgInfo = pm.getPackageInfo(packageInfo.packageName, 0);
//                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
//                String today = sdf.format(ist.getIST());
//                //    subObj.put("app_daily_bytes_rec", mydbHelp.getDailyUsageRec(packageInfo.loadLabel(pm).toString(), today));
//                //   subObj.put("app_daily_bytes_sent", mydbHelp.getDailyUsageSent(packageInfo.loadLabel(pm).toString(), today));
//                subObj.put("app_installdate", pkgInfo.firstInstallTime);
//                //   subObj.put("app_size", packageInfo.);
//                subObj.put("app_version", pkgInfo.versionName);
//                subObj.put("app_name", packageInfo.loadLabel(pm).toString());
//
//                int uid = packageInfo.uid;
//                //adding data sent and received for each app
//                previous=latest;
//                latest=new TrafficSnapshot(this);
//                ArrayList<String> log=new ArrayList<String>();
//                AppTrafficSnapshot latest_rec=latest.apps.get(uid);
//                AppTrafficSnapshot previous_rec=
//                        (previous==null ? null : previous.apps.get(uid));
//
//                emitLog(latest_rec.tag, latest_rec, previous_rec, log);
//                Collections.sort(log);
//
//                for (String row : log) {
//                    //    Log.e("CFA TrafficMonitor", row);
//                    subObj.put("app_data_usage_bytes", row);
//                }
//
//                jarray.put(subObj);
//
//
//            } catch (JSONException e) {
//                e.printStackTrace();
//            } catch (PackageManager.NameNotFoundException e) {
//                e.printStackTrace();
//            }
//        }
//        return jarray;
//    }
//
//    /**
//     * Builds a string with data received and sent for each app and puts all strings into an arraylist to return
//     * @param name
//     * @param latest_rec
//     * @param previous_rec
//     * @param rows
//     */
//    private void emitLog(CharSequence name, AppTrafficSnapshot latest_rec,
//                         AppTrafficSnapshot previous_rec,
//                         ArrayList<String> rows) {
//        if (latest_rec.rx>-1 || latest_rec.tx>-1) {
//            StringBuilder buf=new StringBuilder(name);
//
//            buf.append("=");
//            buf.append(String.valueOf(latest_rec.rx));
//            buf.append(" received");
//
//            if (previous_rec!=null) {
//                buf.append(" (delta=");
//                buf.append(String.valueOf(latest_rec.rx-previous_rec.rx));
//                buf.append(")");
//            }
//
//            buf.append(", ");
//            buf.append(String.valueOf(latest_rec.tx));
//            buf.append(" sent");
//
//            if (previous_rec!=null) {
//                buf.append(" (delta=");
//                buf.append(String.valueOf(latest_rec.tx-previous_rec.tx));
//                buf.append(")");
//            }
//
//            rows.add(buf.toString());
//        }
//    }

}
