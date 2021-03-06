package com.cfap.cfadevicemanager.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.net.Uri;
import android.util.Log;

import com.cfap.cfadevicemanager.DialogActivity;
import com.cfap.cfadevicemanager.dbmodels.DatabaseHelper;
import com.cfap.cfadevicemanager.models.AndroidAgentException;
import com.cfap.cfadevicemanager.models.ApplicationManager;
import com.cfap.cfadevicemanager.models.DeviceAppInfo;
import com.cfap.cfadevicemanager.models.DeviceInfo;
import com.cfap.cfadevicemanager.models.DeviceState;
import com.cfap.cfadevicemanager.models.WiFiConfig;
import com.cfap.cfadevicemanager.services.CFAReceiver;
import com.cfap.cfadevicemanager.services.FetchFromDatabase;
import com.cfap.cfadevicemanager.services.GPSTracker;
import com.cfap.cfadevicemanager.services.ISTDateTime;
import com.cfap.cfadevicemanager.services.MyDeviceAdminReceiver;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
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
 * Created by Shreya Jagarlamudi on 05/10/15.
 */
public class Functions {

    private String TAG = "Functions";
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
    private AlarmManager alarmManager;
    private Context con;

    /**
     * List of Commands
     */
    private String DEVICE_INFO = "DEVICE_INFO";
    private String DEVICE_LOCATION = "DEVICE_LOCATION";
    private String INSTALLED_APPS_LIST = "INSTALLED_APPS_LIST";
    private String LOCK_DEVICE = "LOCK_DEVICE";
    private String WIPE_DATA = "WIPE_DATA";
    private String DELETE_PASSWORD = "DELETE_PASSWORD";
    private String SHOW_NOTIFICATION = "SHOW_NOTIFICATION";
    private String CONTROL_WIFI = "CONTROL_WIFI";
    private String CONTROL_CAMERA = "CONTROL_CAMERA";
    private String INSTALL_NEW_APP = "INSTALL_NEW_APP";
    private String INSTALL_APP_BUNDLE = "INSTALL_APP_BUNDLE";
    private String UNINSTALL_APP = "UNINSTALL_APP";
    private String ENCRYPT_STORED_DATA = "ENCRYPT_STORED_DATA";
    private String RING_DEVICE = "RING_DEVICE";
    private String MUTE_DEVICE = "MUTE_DEVICE";
    private String PASSWORD_POLICY = "PASSWORD_POLICY";
    private String ENTERPRISE_WIPE = "ENTERPRISE_WIPE";
    private String CHANGE_LOCK_CODE = "CHANGE_LOCK_CODE";
    private String BLACKLIST_APPLICATIONS = "BLACKLIST_APPLICATIONS";
    private String PASSCODE_POLICY = "PASSCODE_POLICY";
    private String WEBCLIP = "WEBCLIP";
    private String INSTALL_STORE_APP = "INSTALL_STORE_APP";
    private String POLICY_BUNDLE = "POLICY_BUNDLE";
    private String POLICY_MONITOR = "POLICY_MONITOR";
    private String POLICY_REVOKE = "POLICY_REVOKE";
    private String UNREGISTER = "UNREGISTER";
    private String DATA_USAGE_SING = "DATA_USAGE_SING";
    private String DATA_USAGE_REP = "DATA_USAGE_REP";
    private String STOP_DATA_USAGE_REP = "STOP_DATA_USAGE_REP";
    private String IMEI = "IMEI";
    private static final String LOCATION_INFO_TAG_LONGITUDE = "longitude";
    private static final String LOCATION_INFO_TAG_LATITUDE = "latitude";
    private static final long DAY_MILLISECONDS_MULTIPLIER = 24 * 60 * 60 * 1000;
    private String command;
    private String req_id;

    public Functions(Context context, final JSONObject jsonObject){
        con = context;
        alarmManager = (AlarmManager) con.getSystemService(Context.ALARM_SERVICE);
        this.devicePolicyManager =
                (DevicePolicyManager) con.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.appList = new ApplicationManager(con.getApplicationContext());
        deviceInfo = new DeviceInfo(con.getApplicationContext());
        phoneState = DeviceStateFactory.getDeviceState(con.getApplicationContext(),
                deviceInfo.getSdkVersion());
        gps = new GPSTracker(con.getApplicationContext());
        myDbHelp = DatabaseHelper.getInstance(con.getApplicationContext());
        ist = new ISTDateTime();

        try {

           if(jsonObject!=null){
               command = jsonObject.getString("command");
           }

            if(command.equals(DEVICE_INFO)){
                Log.e(TAG, "Command Received: "+DEVICE_INFO);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendDeviceInfo();
                    }
                });
            }else if(command.equals(DEVICE_LOCATION)){
                Log.e(TAG, "Command Received: "+DEVICE_LOCATION);
                new Thread(new Runnable() {
                @Override
                public void run() {
                    sendLocationInfo();
                }
            });
            }else if(command.equals(INSTALLED_APPS_LIST)){
                Log.e(TAG, "Command Received: "+INSTALLED_APPS_LIST);
                new Thread(new Runnable() {
                @Override
                public void run() {
                    sendInstalledApps();
                }
            });
            }else if(command.equals(LOCK_DEVICE)){
                Log.e(TAG, "Command Received: "+LOCK_DEVICE);
                lockDevice();
            }else if(command.equals(WIPE_DATA)){
                Log.e(TAG, "Command Received: "+WIPE_DATA);
                wipeDevice();
            }else if(command.equals(DELETE_PASSWORD)){
                Log.e(TAG, "Command Received: "+DELETE_PASSWORD);
                clearPassword();
            }else if(command.equals(SHOW_NOTIFICATION)){
                Log.e(TAG, "Command Received: "+SHOW_NOTIFICATION);
                displayNotification(jsonObject);
            }else if(command.equals(CONTROL_WIFI)){
                Log.e(TAG, "Command Received: " + CONTROL_WIFI);
                configureWifi(jsonObject);
            }else if(command.equals(CONTROL_CAMERA)){
                Log.e(TAG, "Command Received: "+CONTROL_CAMERA);
                manageCamera(jsonObject);
            }else if(command.equals(INSTALL_NEW_APP)){
                Log.e(TAG, "Command Received: "+INSTALL_NEW_APP);
                installApplication(jsonObject);
            }else if(command.equals(INSTALL_APP_BUNDLE)){
                Log.e(TAG, "Command Received: "+INSTALL_APP_BUNDLE);

            }else if(command.equals(UNINSTALL_APP)){
                Log.e(TAG, "Command Received: "+UNINSTALL_APP);
                try {
                    uninstallApplication(jsonObject);
                } catch (AndroidAgentException e) {
                    e.printStackTrace();
                }
            }else if(command.equals(ENCRYPT_STORED_DATA)){
                Log.e(TAG, "Command Received: "+ENCRYPT_STORED_DATA);
                try {
                    encryptStorage();
                } catch (AndroidAgentException e) {
                    e.printStackTrace();
                }
            }else if(command.equals(RING_DEVICE)){
                Log.e(TAG, "Command Received: "+RING_DEVICE);
                ringDevice();
            }else if(command.equals(MUTE_DEVICE)){
                Log.e(TAG, "Command Received: "+MUTE_DEVICE);
                muteDevice();
            }else if(command.equals(PASSWORD_POLICY)){
                Log.e(TAG, "Command Received: "+PASSWORD_POLICY);
                setPasswordPolicy(jsonObject);
            }else if(command.equals(ENTERPRISE_WIPE)){
                Log.e(TAG, "Command Received: "+ENTERPRISE_WIPE);
                enterpriseWipe();
            }else if(command.equals(CHANGE_LOCK_CODE)){
                Log.e(TAG, "Command Received: "+CHANGE_LOCK_CODE);
                changeLockCode(jsonObject);
            }else if(command.equals(BLACKLIST_APPLICATIONS)){
                Log.e(TAG, "Command Received: "+BLACKLIST_APPLICATIONS);
                blacklistApps(jsonObject);
            }else if(command.equals(PASSCODE_POLICY)){
                Log.e(TAG, "Command Received: "+PASSCODE_POLICY);

            }else if(command.equals(WEBCLIP)){
                Log.e(TAG, "Command Received: "+WEBCLIP);
                installApplication(jsonObject);
            }else if(command.equals(INSTALL_STORE_APP)){
                Log.e(TAG, "Command Received: "+INSTALL_STORE_APP);
                installApplication(jsonObject);
            }else if(command.equals(POLICY_BUNDLE)){
                Log.e(TAG, "Command Received: "+POLICY_BUNDLE);
            }else if(command.equals(POLICY_MONITOR)){
                Log.e(TAG, "Command Received: "+POLICY_MONITOR);
            }else if(command.equals(POLICY_REVOKE)){
                Log.e(TAG, "Command Received: "+POLICY_REVOKE);
            }else if(command.equals(UNREGISTER)){
                Log.e(TAG, "Command Received: "+UNREGISTER);
                ComponentName demoDeviceAdmin = new ComponentName(con, MyDeviceAdminReceiver.class);
                devicePolicyManager.removeActiveAdmin(demoDeviceAdmin);
                String jStr = "{\"type\": \"enterprise\", \"appIdentifier\": \"com.cfap.cfadevicemanager\"}";
                JSONObject json = null;
                try {
                    json = new JSONObject(jStr);
                    uninstallApplication(json);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (AndroidAgentException e) {
                    e.printStackTrace();
                }
            }else if(command.equals(DATA_USAGE_SING)){
                Log.e(TAG, "Command Received: "+DATA_USAGE_SING);
                //send data stats single time
            }else if(command.equals(DATA_USAGE_REP)){
                Log.e(TAG, "Command Received: "+DATA_USAGE_REP);
                // invoked at 11:59 PM every night
                try {
                    String string1 = "23:59:00";
                    Date time1 = null;
                    time1 = new SimpleDateFormat("HH:mm:ss").parse(string1);
                    Calendar calendar1 = Calendar.getInstance();
                    calendar1.setTime(time1);
                    Intent App_intent = new Intent(con, CFAReceiver.class);
                    App_intent.putExtra("serviceType", "DataUsage");
                    PendingIntent App_PendingIntent = PendingIntent.getBroadcast(con, 0, App_intent, 0);
                    alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar1.getTimeInMillis(), AlarmManager.INTERVAL_DAY, App_PendingIntent);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }else if(command.equals(STOP_DATA_USAGE_REP)){
                Log.e(TAG, "Command Received: "+STOP_DATA_USAGE_REP);
                // cancel alarm
                AlarmManager alarmManager = (AlarmManager) con.getSystemService(Context.ALARM_SERVICE);
                String string1 = "23:59:00";
                Date time1 = null;
                try {
                    time1 = new SimpleDateFormat("HH:mm:ss").parse(string1);
                    Calendar calendar1 = Calendar.getInstance();
                    calendar1.setTime(time1);
                    Intent App_intent = new Intent(con, CFAReceiver.class);
                    App_intent.putExtra("serviceType", "DataUsage");
                    PendingIntent App_PendingIntent = PendingIntent.getBroadcast(con, 0, App_intent, 0);
                    alarmManager.cancel(App_PendingIntent);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve device information.
     */
    public void sendDeviceInfo(){

        JSONObject json = new JSONObject();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a");
        String connTime = formatter.format(ist.getIST());
        try {
            json.put(Constants.Device.IMEI, deviceInfo.getDeviceId());
            json.put(Constants.Device.IMSI, deviceInfo.getIMSINumber());
            json.put(Constants.Device.MODEL, deviceInfo.getDeviceModel());
            json.put(Constants.Device.VENDOR, deviceInfo.getDeviceManufacturer());
            json.put(Constants.Device.OS, deviceInfo.getOsVersion());
            json.put(Constants.Device.NAME, deviceInfo.getDeviceName());
            int batteryLevel = Math.round(phoneState.getBatteryLevel());
            json.put(Constants.Device.BATTERY_LEVEL, String.valueOf(batteryLevel));
            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();
            if (latitude != 0 && longitude !=0) {
                json.put(Constants.Device.MOBILE_DEVICE_LATITUDE, latitude);
                json.put(Constants.Device.MOBILE_DEVICE_LONGITUDE, longitude);
            }
            json.put(Constants.Device.MEMORY_INFO_INTERNAL_TOTAL, String.valueOf(phoneState.getTotalInternalMemorySize()));
            json.put(Constants.Device.MEMORY_INFO_INTERNAL_AVAILABLE, String.valueOf(phoneState.getAvailableInternalMemorySize()));
            json.put(Constants.Device.MEMORY_INFO_EXTERNAL_TOTAL, String.valueOf(phoneState.getTotalExternalMemorySize()));
            json.put(Constants.Device.MEMORY_INFO_EXTERNAL_AVAILABLE, String.valueOf(phoneState.getAvailableExternalMemorySize()));
            json.put(Constants.Device.NETWORK_OPERATOR, deviceInfo.getNetworkOperatorName());
            String jString = json.toString();
            myDbHelp.insertTask(connTime, "DeviceInfo", jString, "pending");
            new FetchFromDatabase(con, "APGOV");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendLocationInfo(){
        double latitude;
        double longitude;
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a");
        String connTime = formatter.format(ist.getIST());
        JSONObject result = new JSONObject();
        latitude = gps.getLatitude();
        longitude = gps.getLongitude();
        try {
            result.put(LOCATION_INFO_TAG_LATITUDE, latitude);
            result.put(LOCATION_INFO_TAG_LONGITUDE, longitude);
            result.put(IMEI, myDbHelp.getImei());
            String jString = result.toString();
            myDbHelp.insertTask(connTime, "LocationInfo", jString, "pending");
            new FetchFromDatabase(con, "APGOV");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void lockDevice(){
        devicePolicyManager.lockNow();
    }

    public void ringDevice(){
        Intent i = new Intent();
        i.setClass(con, DialogActivity.class);
        i.putExtra("type",
                "ring");
        i.putExtra("message",
                "Alarm!!");
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK);
        con.startActivity(i);
    }

    public void wipeDevice(){
        //wipes everything including the MDM app. Need to keep MDM app even after wipe
        devicePolicyManager.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE);
    }

    public void clearPassword(){
        ComponentName demoDeviceAdmin = new ComponentName(con, MyDeviceAdminReceiver.class);
        devicePolicyManager.setPasswordQuality(demoDeviceAdmin,
                DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
        devicePolicyManager.setPasswordMinimumLength(demoDeviceAdmin, 0);
        devicePolicyManager.resetPassword("",
                DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
        devicePolicyManager.lockNow();
        devicePolicyManager.setPasswordQuality(demoDeviceAdmin,
                DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
    }

    public void displayNotification(JSONObject json){
        try {
            String message = json.getString("message");
            if (message != null && !message.isEmpty()) {
                Intent intent = new Intent(con, DialogActivity.class);
                intent.putExtra("message", message);
                intent.putExtra("type",
                        "alert");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK);
                con.startActivity(intent);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void configureWifi(JSONObject wifiData){
        boolean wifiStatus;
        String ssid = null;
        String password = null;
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a");
        String connTime = formatter.format(ist.getIST());
        JSONObject result = new JSONObject();

        if (!wifiData.isNull("ssid")) {
            try {
                ssid = (String) wifiData.get("ssid");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (!wifiData.isNull("password")) {
            try {
                password =
                        (String) wifiData.get("password");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        WiFiConfig config = new WiFiConfig(con.getApplicationContext());

        wifiStatus = config.saveWEPConfig(ssid, password);
        String status;
        if (wifiStatus) {
            status = "true";
            try {
                result.put("status", status);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else {
            status = "false";
            try {
                result.put("status", status);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        String jString = result.toString();
        myDbHelp.insertTask(connTime, "WifiConfig", jString, "pending");
        new FetchFromDatabase(con, "APGOV");
    }

    public void manageCamera(JSONObject json){
        ComponentName cameraAdmin = new ComponentName(con, MyDeviceAdminReceiver.class);
        boolean camFunc = false;
        try {
            camFunc = json.getBoolean("camStatus");
            devicePolicyManager.setCameraDisabled(cameraAdmin, camFunc);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void uninstallApplication(JSONObject json) throws AndroidAgentException {
        String packageName;
        String type;
        try {

            type = json.getString("type");

            if ("webapp".equalsIgnoreCase(type)) {
                String appUrl = json.getString("url");
                String name = json.getString("name");
                String operationType = "uninstall";
                JSONObject result = new JSONObject();
                result.put("identity", appUrl);
                result.put("title", name);
                result.put("type", operationType);
                manageWebClip(result);
            } else {
                packageName = json.getString("appIdentifier");
                appList.uninstallApplication(packageName);
            }

            if (Constants.DEBUG_MODE_ENABLED) {
                Log.d(TAG, "Application started to uninstall");
            }
        } catch (JSONException e) {
            throw new AndroidAgentException("Invalid JSON format.", e);
        }
    }

    private void installApplication(JSONObject data){
        String appUrl;
        String type;
        String name;
        String operationType;
        Log.e(TAG, "installApplication function: "+data);
        try {
            if (!data.isNull("type")) {
                type = data.getString("type");
                if (type.equalsIgnoreCase("enterprise")) {
                    Log.e(TAG, "installApplication function: type- "+"enterprise");
                    appUrl = data.getString("url");
                    appList.installApp(appUrl);
                } else if (type.equalsIgnoreCase("public")) {
                    appUrl = data.getString("appIdentifier");
                    triggerGooglePlayApp(appUrl);

                } else if (type.equalsIgnoreCase("webapp")) {
                    name = data.getString("name");
                    appUrl = data.getString("url");
                    operationType = "install";
                    JSONObject json = new JSONObject();
                    json.put("identity", appUrl);
                    json.put("title", name);
                    json.put("type", operationType);
                    manageWebClip(json);

                } else {
                    throw new AndroidAgentException("Invalid application details");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (AndroidAgentException e) {
            e.printStackTrace();
        }
    }

    /**
     * Open Google Play store application with an application given.
     *
     * @param packageName - Application package name.
     */
    public void triggerGooglePlayApp(String packageName) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse(Constants.GOOGLE_PLAY_APP_URI + packageName));
        con.startActivity(intent);
    }

    public void manageWebClip(JSONObject json){
        String appUrl;
        String title;
        String operationType;
        try {
            appUrl = json.getString("identity");
            title = json.getString("title");
            operationType = json.getString("type");
            if (appUrl != null && title != null) {
                appList.manageWebAppBookmark(appUrl, title, operationType);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (AndroidAgentException e) {
            e.printStackTrace();
        }
    }

    /**
     * Encrypt/Decrypt device storage.
     */
    public void encryptStorage() throws AndroidAgentException {
        try {
            JSONObject result = new JSONObject();
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a");
            String connTime = formatter.format(ist.getIST());
            ComponentName admin = new ComponentName(con, MyDeviceAdminReceiver.class);
            if(devicePolicyManager.getStorageEncryptionStatus() != DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED &&
                    (devicePolicyManager.getStorageEncryptionStatus() == DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE)){
                devicePolicyManager.setStorageEncryption(admin, true);
                Intent intent = new Intent(DevicePolicyManager.ACTION_START_ENCRYPTION);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                con.startActivity(intent);
            }else if (devicePolicyManager.getStorageEncryptionStatus() != DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED &&
                    (devicePolicyManager.getStorageEncryptionStatus() == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE ||
                            devicePolicyManager.getStorageEncryptionStatus() == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING)) {

                devicePolicyManager.setStorageEncryption(admin, false);
            }

            String status;
            if (devicePolicyManager.getStorageEncryptionStatus() !=
                    DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED) {
                status = "true";
                result.put("status", status);

            } else {
                status = "false";
                result.put("status", status);
            }
            String jString = result.toString();
            myDbHelp.insertTask(connTime, "EncryptDecryptStorage", jString, "pending");
            new FetchFromDatabase(con, "APGOV");

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void muteDevice(){
        AudioManager audioManager = (AudioManager) con.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
    }

    public void setPasswordPolicy(JSONObject policyData){
        ComponentName demoDeviceAdmin = new ComponentName(con, MyDeviceAdminReceiver.class);
        int attempts, length, history, specialChars;
        String alphanumeric, complex;
        boolean isAlphanumeric, isComplex;
        long timout;

        try {
            if (!policyData
                    .isNull("maxFailedAttempts") &&
                    policyData.get("maxFailedAttempts") !=
                            null) {
                attempts = policyData.getInt("maxFailedAttempts");
                devicePolicyManager.setMaximumFailedPasswordsForWipe(demoDeviceAdmin, attempts);
            }
            if (!policyData.isNull("minLength") &&
                    policyData.get("minLength") != null) {
                length = policyData.getInt("minLength");
                devicePolicyManager.setPasswordMinimumLength(demoDeviceAdmin, length);
            }
            if (!policyData.isNull("pinHistory") &&
                    policyData.get("pinHistory") != null) {
                history = policyData.getInt("pinHistory");
                devicePolicyManager.setPasswordHistoryLength(demoDeviceAdmin, history);
            }
            if (!policyData
                    .isNull("minComplexChars") &&
                    policyData.get("minComplexChars") !=
                            null) {
                specialChars = policyData.getInt("minComplexChars");
                devicePolicyManager.setPasswordMinimumSymbols(demoDeviceAdmin, specialChars);
            }
            if (!policyData
                    .isNull("requireAlphanumeric") &&
                    policyData
                            .get("requireAlphanumeric") !=
                            null) {
                if (policyData.get("requireAlphanumeric") instanceof String) {
                    alphanumeric =
                            (String) policyData.get("requireAlphanumeric");
                    if (alphanumeric
                            .equals("true")) {
                        devicePolicyManager.setPasswordQuality(demoDeviceAdmin,
                                DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC);
                    }
                } else if (policyData.get("requireAlphanumeric") instanceof Boolean) {
                    isAlphanumeric =
                            policyData.getBoolean("requireAlphanumeric");
                    if (isAlphanumeric) {
                        devicePolicyManager.setPasswordQuality(demoDeviceAdmin,
                                DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC);
                    }
                }
            }
            if (!policyData.isNull("allowSimple") &&
                    policyData.get("allowSimple") != null) {

                if (policyData.get("allowSimple") instanceof String) {
                    complex =
                            (String) policyData.get("allowSimple");
                    if (!complex.equals("true")) {
                        devicePolicyManager.setPasswordQuality(demoDeviceAdmin,
                                DevicePolicyManager.PASSWORD_QUALITY_COMPLEX);
                    }
                } else if (policyData.get("allowSimple") instanceof Boolean) {
                    isComplex =
                            policyData.getBoolean("allowSimple");
                    if (!isComplex) {
                        devicePolicyManager.setPasswordQuality(demoDeviceAdmin,
                                DevicePolicyManager.PASSWORD_QUALITY_COMPLEX);
                    }
                }
            }
            if (!policyData.isNull("maxPINAgeInDays") &&
                    policyData.get("maxPINAgeInDays") !=
                            null) {
                int daysOfExp = policyData.getInt("maxPINAgeInDays");
                timout = daysOfExp * DAY_MILLISECONDS_MULTIPLIER;
                devicePolicyManager.setPasswordExpirationTimeout(demoDeviceAdmin, timout);
            }

        }catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void changeLockCode(JSONObject lockData){
        ComponentName demoDeviceAdmin = new ComponentName(con, MyDeviceAdminReceiver.class);
        devicePolicyManager.setPasswordMinimumLength(demoDeviceAdmin, 3);
        String password = null;
        if (!lockData.isNull("lockCode")) {
            try {
                password =
                        (String) lockData.get("lockCode");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (password != null && !password.isEmpty()) {
            devicePolicyManager.resetPassword(password,
                    DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
            devicePolicyManager.lockNow();
        }
    }

    public void enterpriseWipe(){

    }

    public void blacklistApps(JSONObject resultApp){
        ArrayList<DeviceAppInfo> apps = new ArrayList<>(appList.getInstalledApps().values());
        JSONArray appList = new JSONArray();
        JSONArray blacklistApps = new JSONArray();
        String identity;
        try {
            if (!resultApp.isNull("appIdentifier")) {
                blacklistApps = resultApp.getJSONArray("appIdentifier");
            }

        } catch (JSONException e) {
            try {
                throw new AndroidAgentException("Invalid JSON format.", e);
            } catch (AndroidAgentException e1) {
                e1.printStackTrace();
            }
        }
        for (int i = 0; i < blacklistApps.length(); i++) {
            try {
                identity = blacklistApps.getString(i);
                for (DeviceAppInfo app : apps) {
                    JSONObject result = new JSONObject();

                    result.put("name", app.getAppname());
                    result.put("package",
                            app.getPackagename());
                    if (identity.trim().equals(app.getPackagename())) {
                        result.put("notviolated", false);
                        result.put("package",
                                app.getPackagename());
                    } else {
                        result.put("notviolated", true);
                    }
                    appList.put(result);
                    Log.e(TAG, "blacklist result: " + result);
                    Log.e(TAG, "blacklist appList: "+appList);
                }
            } catch (JSONException e) {
                try {
                    throw new AndroidAgentException("Invalid JSON format.", e);
                } catch (AndroidAgentException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public void sendInstalledApps(){

        final PackageManager pm = con.getPackageManager();
        //get a list of installed apps.
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {
            //   Log.e(TAG, "APP NAME: "+packageInfo.loadLabel(pm));
            JSONObject subObj = new JSONObject();

            try {
                PackageInfo pkgInfo = pm.getPackageInfo(packageInfo.packageName, 0);
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a");
                String connTime = formatter.format(ist.getIST());
                subObj.put("app_installdate", pkgInfo.firstInstallTime);
                subObj.put("app_version", pkgInfo.versionName);
                subObj.put("app_name", packageInfo.loadLabel(pm).toString());
                String jString = subObj.toString();
                myDbHelp.insertTask(connTime, "InstalledApps", jString, "pending");
                new FetchFromDatabase(con, "APGOV");
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendDataUsageOnce(){

    }

}
