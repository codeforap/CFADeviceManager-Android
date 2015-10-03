package com.cfap.cfadevicemanager.dbmodels;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cfap.cfadevicemanager.models.AppTrafficRecord;
import com.cfap.cfadevicemanager.models.AppTrafficSnapshot;
import com.cfap.cfadevicemanager.models.TrafficSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by PraveenKatha on 29/09/15.
 */
public class DataTrackerDBHelper extends DatabaseHelper {

    private static DataTrackerDBHelper mInstance;

    public DataTrackerDBHelper(Context applicationContext) {
        super(applicationContext);
    }

    public static synchronized DataTrackerDBHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new DataTrackerDBHelper(context.getApplicationContext());
        }
        return mInstance;
    }

    public void insertAppTrafficRecord(TrafficSnapshot trafficSnapshot) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        String day = trafficSnapshot.getDay();
        for (AppTrafficSnapshot appTrafficSnapshot : trafficSnapshot.getApps().values()) {
            ContentValues values = new ContentValues();
            values.put(DataTrackerDBModel.COLUMN_NAME_APP_UID, appTrafficSnapshot.getUid());
            values.put(DataTrackerDBModel.COLUMN_NAME_APP_NAME, appTrafficSnapshot.getName());
            values.put(DataTrackerDBModel.COLUMN_NAME_APP_ICON, appTrafficSnapshot.getIconDrawable());
            values.put(DataTrackerDBModel.COLUMN_NAME_APP_MOBILE_DATA, 0);
            values.put(DataTrackerDBModel.COLUMN_NAME_APP_WIFI_DATA, 0);
            values.put(DataTrackerDBModel.COLUMN_NAME_APP_RECENT_DATA_STAMP, appTrafficSnapshot.getAppDataStamp());
            values.put(DataTrackerDBModel.COLUMN_NAME_DAY, day);
            long row = db.insert(DataTrackerDBModel.TABLE_NAME, null, values);
            System.out.println(row);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void updateAppTrafficRecords(List<AppTrafficRecord> appTrafficRecords, String day, boolean isWifiData) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        int rows = 0;
        for (AppTrafficRecord appTrafficRecord : appTrafficRecords) {
            ContentValues values = new ContentValues();

            if (isWifiData)
                values.put(DataTrackerDBModel.COLUMN_NAME_APP_WIFI_DATA, appTrafficRecord.getWifiData());
            else
                values.put(DataTrackerDBModel.COLUMN_NAME_APP_MOBILE_DATA, appTrafficRecord.getNetworkData());

            values.put(DataTrackerDBModel.COLUMN_NAME_APP_RECENT_DATA_STAMP, appTrafficRecord.getAppDataStamp());

            String[] whereArgs = {appTrafficRecord.getDay(), "" + appTrafficRecord.getUid()};
            String where = DataTrackerDBModel.COLUMN_NAME_DAY + "=? AND " + DataTrackerDBModel.COLUMN_NAME_APP_UID + "=?";
            rows += db.update(DataTrackerDBModel.TABLE_NAME, values, where, whereArgs);
        }

        System.out.println("Apps Updated records "+rows);
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public boolean isSnapshotExistForDay(String day) {
        SQLiteDatabase db = getReadableDatabase();
        String palsQuery = "SELECT * FROM "
                + DataTrackerDBModel.TABLE_NAME + " WHERE " + DataTrackerDBModel.COLUMN_NAME_DAY + " = ?";
        Cursor cr = db.rawQuery(palsQuery, new String[]{day});
        if (cr == null)
            return false;
        boolean b = cr.getCount() > 0;
        cr.close();
        return b;
    }

    public ArrayList<AppTrafficRecord> getAllAppsTrafficToday(String day) {

        ArrayList<AppTrafficRecord> appTrafficRecords = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DataTrackerDBModel.TABLE_NAME + " WHERE "
                + DataTrackerDBModel.COLUMN_NAME_DAY + " = ?", new String[]{day});

        if (cursor != null && cursor.moveToFirst()) {
            System.out.println("Apps records size " + cursor.getCount());
            do {
                appTrafficRecords.add(getAppTrafficRecordFromCursor(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }

        return appTrafficRecords;
    }


    public ArrayList<AppTrafficRecord> getAllAppsTrafficTodayUI(String day) {

        ArrayList<AppTrafficRecord> appTrafficRecords = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DataTrackerDBModel.TABLE_NAME + " WHERE "
                + DataTrackerDBModel.COLUMN_NAME_DAY + " = ?", new String[]{day});
        AppTrafficRecord appTrafficRecord;
        if (cursor != null && cursor.moveToFirst()) {

            do {
                appTrafficRecord = getAppTrafficRecordFromCursor(cursor);
                if (appTrafficRecord.getNetworkData() > 0 || appTrafficRecord.getWifiData() > 0)
                    appTrafficRecords.add(appTrafficRecord);
            } while (cursor.moveToNext());
            cursor.close();
        }
        System.out.println("Apps records size " + appTrafficRecords.size());
        return appTrafficRecords;
    }

    private AppTrafficRecord getAppTrafficRecordFromCursor(Cursor cr) {

        AppTrafficRecord appTrafficRecord = new AppTrafficRecord();

        appTrafficRecord.setName(cr.getString(cr.getColumnIndex(DataTrackerDBModel.COLUMN_NAME_APP_NAME)));
        appTrafficRecord.setDay(cr.getString(cr.getColumnIndex(DataTrackerDBModel.COLUMN_NAME_DAY)));
        appTrafficRecord.setNetworkData(cr.getLong(cr.getColumnIndex(DataTrackerDBModel.COLUMN_NAME_APP_MOBILE_DATA)));
        appTrafficRecord.setWifiData(cr.getLong(cr.getColumnIndex(DataTrackerDBModel.COLUMN_NAME_APP_WIFI_DATA)));
        appTrafficRecord.setUid(cr.getInt(cr.getColumnIndex(DataTrackerDBModel.COLUMN_NAME_APP_UID)));
        appTrafficRecord.setAppDataStamp(cr.getLong(cr.getColumnIndex(DataTrackerDBModel.COLUMN_NAME_APP_RECENT_DATA_STAMP)));

        return appTrafficRecord;

    }
}
