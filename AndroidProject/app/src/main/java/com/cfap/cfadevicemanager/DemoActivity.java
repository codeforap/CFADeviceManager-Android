package com.cfap.cfadevicemanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cfap.cfadevicemanager.dbmodels.DataTrackerDBModel;
import com.cfap.cfadevicemanager.models.AppTrafficRecord;
import com.cfap.cfadevicemanager.services.AppTrackerService;
import com.cfap.cfadevicemanager.utils.Intents;

import java.io.IOException;
import java.util.List;

/**
 * Created by PraveenKatha on 30/09/15.
 */
public class DemoActivity extends AppCompatActivity{

    private RecyclerView recyclerView;
    private DatabaseHelper myDbHelp;
    private MyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_activity);

        myDbHelp = DatabaseHelper.getInstance(getApplicationContext());
        try {
            myDbHelp.createDataBase();

        } catch (IOException e) {
            // TODO Auto-generated catch block;
            e.printStackTrace();
        }
        recyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);


        adapter = new MyAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter.setData(DataTrackerDBModel.getAppRecordsForToday(this));
        LocalBroadcastManager.getInstance(this).registerReceiver(appDataUpdateStatusReceiver,
                new IntentFilter(Intents.APP_DATA_UPDATED_EVENT));

    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent i = new Intent(this,AppTrackerService.class);
        i.setAction("APP_DATA_REFRESH");
        startService(i);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(appDataUpdateStatusReceiver);
    }

    private void refreshData() {
        adapter.setData(DataTrackerDBModel.getAppRecordsForToday(this));
    }

    class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

        private List<AppTrafficRecord> trafficRecords;

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
            View itemView = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.list_item, parent, false);
            return new MyAdapter.ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int i) {
            AppTrafficRecord appTrafficRecord = trafficRecords.get(i);
            viewHolder.appNameTv.setText(""+appTrafficRecord.getName());
            viewHolder.wifiDataTv.setText("Wifi data: "+appTrafficRecord.getWifiData());
            viewHolder.mobileDataTv.setText("Mobile Data: "+appTrafficRecord.getNetworkData());
        }

        public void setData(List<AppTrafficRecord> trafficRecords) {
            this.trafficRecords = trafficRecords;
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            if(trafficRecords == null)
                return 0;
            return trafficRecords.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView iv;
            TextView appNameTv;
            TextView mobileDataTv;
            TextView wifiDataTv;

            public ViewHolder(View itemView) {
                super(itemView);
                iv = (ImageView) itemView.findViewById(R.id.app_icon);
                appNameTv = (TextView) itemView.findViewById(R.id.app_name);
                mobileDataTv = (TextView) itemView.findViewById(R.id.mobile_data);
                wifiDataTv = (TextView) itemView.findViewById(R.id.wifi_data);
            }
        }
    }

    private BroadcastReceiver appDataUpdateStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            Toast.makeText(DemoActivity.this,"Data Updated",Toast.LENGTH_SHORT).show();
            refreshData();
        }
    };


}
