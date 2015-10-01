package com.cfap.cfadevicemanager.models;

/**
 * Created by PraveenKatha on 29/09/15.
 */
public class AppTrafficRecord {

    private String name;
    private int uid;
    private long networkData;
    private long wifiData;
    private String day;
    private long appDataStamp;

    public AppTrafficRecord() {
    }

    public AppTrafficRecord(String name) {
        this.name = name;
        this.wifiData = 20;
        this.networkData = 30;

    }

    public String getName() {
        return name;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public float getNetworkData() {
        return networkData;
    }

    public void setNetworkData(long networkData) {
        this.networkData = networkData;
    }

    public void setWifiData(long wifiData) {
        this.wifiData = wifiData;
    }

    public void setAppDataStamp(long appDataStamp) {
        this.appDataStamp = appDataStamp;
    }

    public float getWifiData() {
        return wifiData;
    }

    public long getAppDataStamp() {
        return appDataStamp;
    }

    public void updateData(AppTrafficSnapshot snapshot, boolean isWifiData, boolean onShutDown) {

        if (isWifiData) {
            wifiData += snapshot.getAppDataStamp() - appDataStamp;
        } else {
            networkData += snapshot.getAppDataStamp() - appDataStamp;
        }

        appDataStamp = onShutDown ? 0 : snapshot.getAppDataStamp();
    }

}
