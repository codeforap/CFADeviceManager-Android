package com.cfap.cfadevicemanager.services;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.cfap.cfadevicemanager.DatabaseHelper;
import com.cfap.cfadevicemanager.DialogActivity;
import com.cfap.cfadevicemanager.R;
import com.cfap.cfadevicemanager.models.AndroidAgentException;
import com.cfap.cfadevicemanager.models.ApplicationManager;
import com.cfap.cfadevicemanager.models.DeviceInfo;
import com.cfap.cfadevicemanager.models.DeviceState;
import com.cfap.cfadevicemanager.models.Preference;
import com.cfap.cfadevicemanager.models.WiFiConfig;
import com.cfap.cfadevicemanager.utils.Constants;
import com.cfap.cfadevicemanager.utils.DeviceStateFactory;

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

import java.text.SimpleDateFormat;

/**
 * Created by Shreya Jagarlamudi on 18/09/15.
 */
public class MyMqttService extends Service{

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
    private static final String LOCATION_INFO_TAG_LONGITUDE = "longitude";
    private static final String LOCATION_INFO_TAG_LATITUDE = "latitude";
    private static final long DAY_MILLISECONDS_MULTIPLIER = 24 * 60 * 60 * 1000;


    public MyMqttService(){

    }

    public MyMqttService(Context c){
       // this.context = c;
    }

    @Override
    public void onCreate() {
        super.onCreate();
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

     /*   String jString = "{\"command\": \"INSTALL_NEW_APP\", \"type\": \"enterprise\", \"appIdentifier\": \"com.shreyaj.spree\"}";
        try {
            JSONObject jsonObject = new JSONObject(jString);
            if(jsonObject.getString("command").equals(INSTALL_NEW_APP)){
                Log.e(TAG, "Command: " + INSTALL_NEW_APP);
                try {
                    uninstallApplication(jsonObject);
                } catch (AndroidAgentException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } */
        return START_STICKY;
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
            new FetchFromDatabase(this, "myimei");
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
            String jString = result.toString();
            myDbHelp.insertTask(connTime, "LocationInfo", jString, "pending");
            new FetchFromDatabase(this, "myimei");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void lockDevice(){
        devicePolicyManager.lockNow();
    }

    public void ringDevice(){
        Intent i = new Intent();
        i.setClass(this, DialogActivity.class);
        i.putExtra("type",
                "ring");
        i.putExtra("message",
                "Alarm!!");
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    public void wipeDevice(){
        //wipes everything including the MDM app. Need to keep MDM app even after wipe
        devicePolicyManager.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE);
    }

    public void clearPassword(){
        ComponentName demoDeviceAdmin = new ComponentName(this, MyDeviceAdminReceiver.class);
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
                Intent intent = new Intent(this, DialogActivity.class);
                intent.putExtra("message", message);
                intent.putExtra("type",
                        "alert");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
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
        WiFiConfig config = new WiFiConfig(getApplicationContext());

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
        new FetchFromDatabase(this, "myimei");
    }

    public void manageCamera(JSONObject json){
        ComponentName cameraAdmin = new ComponentName(this, MyDeviceAdminReceiver.class);
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

        try {
            if (!data.isNull("type")) {
                type = data.getString("type");

                if (type.equalsIgnoreCase("enterprise")) {
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
        startActivity(intent);
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
            ComponentName admin = new ComponentName(this, MyDeviceAdminReceiver.class);
            if(devicePolicyManager.getStorageEncryptionStatus() != DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED &&
                    (devicePolicyManager.getStorageEncryptionStatus() == DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE)){
                devicePolicyManager.setStorageEncryption(admin, true);
                Intent intent = new Intent(DevicePolicyManager.ACTION_START_ENCRYPTION);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
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
            new FetchFromDatabase(this, "myimei");

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void muteDevice(){
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
    }

    public void setPasswordPolicy(JSONObject policyData){
        ComponentName demoDeviceAdmin = new ComponentName(this, MyDeviceAdminReceiver.class);
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
        ComponentName demoDeviceAdmin = new ComponentName(this, MyDeviceAdminReceiver.class);
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
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        @Override
        public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
            JSONObject jsonObject = new JSONObject(new String( mqttMessage.getPayload()));
            Log.e(TAG, "mqtt message arrived"+jsonObject.toString());

            if(jsonObject.getString("command").equals(DEVICE_INFO)){
                Log.e(TAG, "Command Received: "+DEVICE_INFO);
            }else if(jsonObject.getString("command").equals(DEVICE_LOCATION)){
                Log.e(TAG, "Command Received: "+DEVICE_LOCATION);
            }else if(jsonObject.getString("command").equals(INSTALLED_APPS_LIST)){
                Log.e(TAG, "Command Received: "+INSTALLED_APPS_LIST);
            }else if(jsonObject.getString("command").equals(LOCK_DEVICE)){
                Log.e(TAG, "Command Received: "+LOCK_DEVICE);
            }else if(jsonObject.getString("command").equals(WIPE_DATA)){
                Log.e(TAG, "Command Received: "+WIPE_DATA);
            }else if(jsonObject.getString("command").equals(DELETE_PASSWORD)){
                Log.e(TAG, "Command Received: "+DELETE_PASSWORD);
            }else if(jsonObject.getString("command").equals(SHOW_NOTIFICATION)){
                Log.e(TAG, "Command Received: "+SHOW_NOTIFICATION);
            }else if(jsonObject.getString("command").equals(CONTROL_WIFI)){
                Log.e(TAG, "Command Received: "+CONTROL_WIFI);
            }else if(jsonObject.getString("command").equals(CONTROL_CAMERA)){
                Log.e(TAG, "Command Received: "+CONTROL_CAMERA);
            }else if(jsonObject.getString("command").equals(INSTALL_NEW_APP)){
                Log.e(TAG, "Command Received: "+INSTALL_NEW_APP);
            }else if(jsonObject.getString("command").equals(INSTALL_APP_BUNDLE)){
                Log.e(TAG, "Command Received: "+INSTALL_APP_BUNDLE);
            }else if(jsonObject.getString("command").equals(UNINSTALL_APP)){
                Log.e(TAG, "Command Received: "+UNINSTALL_APP);
            }else if(jsonObject.getString("command").equals(ENCRYPT_STORED_DATA)){
                Log.e(TAG, "Command Received: "+ENCRYPT_STORED_DATA);
            }else if(jsonObject.getString("command").equals(RING_DEVICE)){
                Log.e(TAG, "Command Received: "+RING_DEVICE);
            }else if(jsonObject.getString("command").equals(MUTE_DEVICE)){
                Log.e(TAG, "Command Received: "+MUTE_DEVICE);
            }else if(jsonObject.getString("command").equals(PASSWORD_POLICY)){
                Log.e(TAG, "Command Received: "+PASSWORD_POLICY);
            }else if(jsonObject.getString("command").equals(ENTERPRISE_WIPE)){
                Log.e(TAG, "Command Received: "+ENTERPRISE_WIPE);
            }else if(jsonObject.getString("command").equals(CHANGE_LOCK_CODE)){
                Log.e(TAG, "Command Received: "+CHANGE_LOCK_CODE);
            }else if(jsonObject.getString("command").equals(BLACKLIST_APPLICATIONS)){
                Log.e(TAG, "Command Received: "+BLACKLIST_APPLICATIONS);
            }else if(jsonObject.getString("command").equals(PASSCODE_POLICY)){
                Log.e(TAG, "Command Received: "+PASSCODE_POLICY);
            }else if(jsonObject.getString("command").equals(WEBCLIP)){
                Log.e(TAG, "Command Received: "+WEBCLIP);
            }else if(jsonObject.getString("command").equals(INSTALL_STORE_APP)){
                Log.e(TAG, "Command Received: "+INSTALL_STORE_APP);
            }else if(jsonObject.getString("command").equals(POLICY_BUNDLE)){
                Log.e(TAG, "Command Received: "+POLICY_BUNDLE);
            }else if(jsonObject.getString("command").equals(POLICY_MONITOR)){
                Log.e(TAG, "Command Received: "+POLICY_MONITOR);
            }else if(jsonObject.getString("command").equals(POLICY_REVOKE)){
                Log.e(TAG, "Command Received: "+POLICY_REVOKE);
            }else if(jsonObject.getString("command").equals(UNREGISTER)){
                Log.e(TAG, "Command Received: "+UNREGISTER);
            }

        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
            Log.e(TAG, "mqtt delivery complete");
        }
    }
}
