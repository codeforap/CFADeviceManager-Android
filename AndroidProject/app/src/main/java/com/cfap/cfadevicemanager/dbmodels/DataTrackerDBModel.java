package com.cfap.cfadevicemanager.dbmodels;

import android.content.Context;
import android.provider.BaseColumns;
import com.cfap.cfadevicemanager.models.AppTrafficRecord;
import com.cfap.cfadevicemanager.models.TrafficSnapshot;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by PraveenKatha on 29/09/15.
 */
public class DataTrackerDBModel implements BaseColumns {

    public static final String TABLE_NAME = "app_data_tracker";
    public static final String COLUMN_NAME_APP_NAME = "app_name";
    public static final String COLUMN_NAME_APP_ICON = "app_icon";
    public static final String COLUMN_NAME_APP_UID = "app_uid";
    public static final String COLUMN_NAME_APP_MOBILE_DATA = "app_mobile_data";
    public static final String COLUMN_NAME_APP_WIFI_DATA = "app_wifi_data";
    public static final String COLUMN_NAME_APP_RECENT_DATA_STAMP = "app_recent_data_stamp";
    public static final String COLUMN_NAME_DAY = "day";

    public static void addOrUpdateTrafficSnapshot(TrafficSnapshot trafficSnapshot, Context context, boolean isWifi, boolean onShutDown) {
        DataTrackerDBHelper dbHelper = DataTrackerDBHelper.getInstance(context);
        if (!dbHelper.isSnapshotExistForDay(trafficSnapshot.getDay())) {
            dbHelper.insertAppTrafficRecord(trafficSnapshot);
        } else {
            ArrayList<AppTrafficRecord> appTrafficRecords = dbHelper.getAllAppsTrafficToday(trafficSnapshot.getDay());

            for (AppTrafficRecord appTrafficRecord : appTrafficRecords) {
                appTrafficRecord.updateData(trafficSnapshot.getSnapshot(appTrafficRecord.getUid()), isWifi, onShutDown);
            }

            dbHelper.updateAppTrafficRecords(appTrafficRecords, trafficSnapshot.getDay(), isWifi);
        }
    }


    public static List<AppTrafficRecord> getAppRecordsForToday(Context context) {
        DataTrackerDBHelper dbHelper = DataTrackerDBHelper.getInstance(context);
        return dbHelper.getAllAppsTrafficTodayUI(DateTime.now().toString("dd-MMM-yyyy"));
    }
}
