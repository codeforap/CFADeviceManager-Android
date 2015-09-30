package com.cfap.cfadevicemanager.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by PraveenKatha on 30/09/15.
 */
public class SharedPrefUtils {

    public final static String PREF_KEY = "DATA_TRACKER";
    public final static String IS_FIRST_TIME_APP_INSTALL = "IS_FIRST_TIME_APP_INSTALL";
    private static final String WIFI_STATUS_TILL_NOW = "WIFI_STATUS_TILL_NOW";

    public static boolean isFirstTimeInstall(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        boolean isFirstTimeAppInstall = preferences.getBoolean(IS_FIRST_TIME_APP_INSTALL, true);
        if (isFirstTimeAppInstall) {
            preferences.edit().putBoolean(IS_FIRST_TIME_APP_INSTALL, false).apply();
            return true;
        }
        return false;
    }

    public static boolean getCurrentWifiStatus(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return preferences.getBoolean(WIFI_STATUS_TILL_NOW, true);
    }

    public static void updateCurrentWifiStatus(boolean wifiStatus, Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(WIFI_STATUS_TILL_NOW, wifiStatus).apply();
    }
}
