package com.cfap.cfadevicemanager;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Shreya Jagarlamudi on 03/09/15.
 */
public class LocationDetector implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private String TAG = "LocationDetector";
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private Context context;
    private String currLocation; // string version of curr location or last known location
    private String lastLocTime; // time of curr location or last known location
    private long UPDATE_INTERVAL = (60000)*20; // updates location every 20 mins
    private long FASTEST_INTERVAL = (60000)*15;
    FusedLocationProviderApi fusedLocationProviderApi;

    public LocationDetector(Context c){
        context = c;
      /*  buildGoogleApiClient();
        if(mGoogleApiClient!= null){
            mGoogleApiClient.connect();
        }else{
            Log.e(TAG, "not connected");
        }
        mLocationRequest = new LocationRequest();

        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL); */

        getLocation();

    }

    private void getLocation() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(60000);
        mLocationRequest.setFastestInterval(60000);
        fusedLocationProviderApi = LocationServices.FusedLocationApi;
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.e(TAG, "in onConnected");
        fusedLocationProviderApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
      //  mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
       //         mGoogleApiClient);
        mLastLocation = fusedLocationProviderApi.getLastLocation(
                         mGoogleApiClient);
        if (mLastLocation != null) {
            setCurrLocation(String.valueOf(mLastLocation.getLatitude()) + ", " + String.valueOf(mLastLocation.getLongitude()));
            Long time = mLastLocation.getTime();
            setLastLocTime(time);
            Log.e(TAG, currLocation+" "+getLastLocTime());
        }
    }

    private void setCurrLocation(String loc){
        currLocation = loc;
    }
    public String getCurrLocation(){
        return currLocation;
    }

    private void setLastLocTime(long t){
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a");
        Date d = new Date(t);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+05:30"));
        lastLocTime = sdf.format(d);
    }

    public String getLastLocTime(){
        return lastLocTime;
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "in onConnectionSuspended");
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if (mLastLocation != null) {
            setCurrLocation(String.valueOf(mLastLocation.getLatitude())+", "+String.valueOf(mLastLocation.getLongitude()));
            setLastLocTime(mLastLocation.getTime());
            Log.e(TAG, "currLocation changed: "+currLocation+" "+getLastLocTime());
        }
        // NEED TO UPDATE TO DATABASE & START SENDPENDINGTASKS
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "in onConnectionFailed");
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
}
