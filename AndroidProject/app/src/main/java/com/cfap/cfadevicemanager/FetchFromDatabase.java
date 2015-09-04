package com.cfap.cfadevicemanager;

import android.content.Context;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Shreya Jagarlamudi on 03/09/15.
 */
public class FetchFromDatabase {

    private Context context;
    private DatabaseHelper myDbHelp;

    public FetchFromDatabase(Context c){
        context = c;
        myDbHelp = DatabaseHelper.getInstance(context);
        fetchNsend();
    }

    private void fetchNsend(){
        ArrayList<String> pendingTasks = myDbHelp.getPendingJsons();
        if(pendingTasks.size()>0){
            for(int i=0; i<pendingTasks.size(); i++){
                // push each task to server
                try {
                    String s = pendingTasks.get(i);
                    JSONObject jObj = new JSONObject(s);
                    SendToServer sts = new SendToServer(context, jObj);
                    myDbHelp.updateTaskStatus(s, "sent");
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
