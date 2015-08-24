package com.cfap.cfadevicemanager;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Shreya Jagarlamudi on 27/07/15.
 */

/**
 * This class is the main class for the entire application. As soon as the user launches application, oncreate method of this
 * class is triggered before anything else.
 * This class starts the MQTT service on launch first time we open the app ever and also contains UI elements to display
 * messages to the user. The UI will be updated from our CfaService class using Broadcast Receiver.
 */
public class MainActivity extends Activity {

    private String TAG = "MainActivity";
   // private Intent intent;
    private ImageView logoview;
    private TextView nameview;
    private TextView maintv;
   // private TextView usernametv;
  //  private TextView passwordtv;
    private EditText usernameet;
    private EditText passwordet;
    private Button submit;
    private Button logout;
    private SharedPreferences sharedpreferences;
    GlobalState gs;

    private final String MyPREFERENCES = "CfaPrefs" ;
    private final String usernamePref = "cfausername";
    private final String passwordPref = "cfapassword";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
      //  intent = new Intent(this, CFAService.class);
        gs = (GlobalState) getApplication();

        logoview = (ImageView) findViewById(R.id.logoview);
        nameview = (TextView) findViewById(R.id.nameview);
      //  usernametv = (TextView) findViewById(R.id.usernametv);
        usernameet = (EditText) findViewById(R.id.usernameet);
      //  passwordtv = (TextView) findViewById(R.id.pwtv);
        passwordet = (EditText) findViewById(R.id.pwet);
        submit = (Button) findViewById(R.id.submit);
        maintv = (TextView) findViewById(R.id.maintv);
        logout = (Button) findViewById(R.id.logout);
        logout.setVisibility(View.GONE);

        String display = "DETAILS: \n\nFetching Data...";
        maintv.setTextSize(20);
       // maintv.setText(display + " " + gs.getjStr());

        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        Log.e(TAG, "saved username: "+sharedpreferences.getString(usernamePref, "noValun"));
        Log.e(TAG, "saved pw: "+sharedpreferences.getString(passwordPref, "noValpw"));

        if(sharedpreferences.getString(usernamePref, "noValun").equals("admin") &&
                sharedpreferences.getString(passwordPref, "noValpw").equals("cfap")){
            Log.e(TAG, "username & pw already saved");
            logoview.setVisibility(View.GONE);
            nameview.setVisibility(View.GONE);
          //  usernametv.setVisibility(View.GONE);
            usernameet.setVisibility(View.GONE);
          //  passwordtv.setVisibility(View.GONE);
            passwordet.setVisibility(View.GONE);
            submit.setVisibility(View.GONE);
          //  maintv.setText(display + " " + gs.getjStr());
            maintv.setText("Thank you for registering your device with the Government of Andhra Pradesh");
            logout.setVisibility(View.VISIBLE);
            if(isMyServiceRunning(CFAService.class)==false){
                Log.e(TAG, "service not running, starting now!");
                startMqttService();
            }

        }

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if((usernameet.getText().toString().equals("admin") && passwordet.getText().toString().equals("cfap"))
                        ){
                    // we only start service if it is already not running
                    logoview.setVisibility(View.GONE);
                    nameview.setVisibility(View.GONE);
                  //  usernametv.setVisibility(View.GONE);
                    usernameet.setVisibility(View.GONE);
                   // passwordtv.setVisibility(View.GONE);
                    passwordet.setVisibility(View.GONE);
                    submit.setVisibility(View.GONE);
                    logout.setVisibility(View.VISIBLE);

                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString(usernamePref, "admin");
                    editor.putString(passwordPref, "cfap");
                    editor.commit();

                    if(isMyServiceRunning(CFAService.class)==false){
                        Log.e(TAG, "service not running, starting now!");
                        startMqttService();
                    }
                  //  usernameet.setText("");
                //    passwordet.setText("");
                //    submit.setClickable(false);
                } else{
                    usernameet.setText("");
                    passwordet.setText("");
                    Toast.makeText(MainActivity.this, "The login details you entered are incorrect. Please try again!", 3).show();
                }
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString(usernamePref, "noValun");
                editor.putString(passwordPref, "noValpw");
                editor.commit();
                maintv.setVisibility(View.GONE);
                logout.setVisibility(View.GONE);
                logoview.setVisibility(View.VISIBLE);
                nameview.setVisibility(View.VISIBLE);
             //   usernametv.setVisibility(View.VISIBLE);
                usernameet.setVisibility(View.VISIBLE);
              //  passwordtv.setVisibility(View.VISIBLE);
                passwordet.setVisibility(View.VISIBLE);
                submit.setVisibility(View.VISIBLE);
                usernameet.setText("");
                passwordet.setText("");
                //if user logs out, stop sending data to server
                if(isMyServiceRunning(CFAService.class)==true){
                    Intent intent = new Intent(MainActivity.this, CFAService.class);
                    stopService(intent);
                }
            }
        });


    }

    public void startMqttService(){
        Intent intent = new Intent(MainActivity.this, CFAService.class);
        startService(intent);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI(intent);
        }
    };

    public void updateUI(Intent in){
        logoview.setVisibility(View.GONE);
        nameview.setVisibility(View.GONE);
      //  usernametv.setVisibility(View.GONE);
        usernameet.setVisibility(View.GONE);
      //  passwordtv.setVisibility(View.GONE);
        passwordet.setVisibility(View.GONE);
        submit.setVisibility(View.GONE);
        maintv.setTextSize(20);
        String text = in.getStringExtra("jstring");
      //  maintv.setText("DETAILS: \n\n"+gs.getjStr());
        maintv.setText("Thank you for registering this device with the Government of Andhra Pradesh, India");
        maintv.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //startMqttService();
        registerReceiver(broadcastReceiver, new IntentFilter(CFAService.BROADCAST_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
