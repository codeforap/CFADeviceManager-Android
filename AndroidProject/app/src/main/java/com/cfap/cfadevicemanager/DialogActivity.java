package com.cfap.cfadevicemanager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cfap.cfadevicemanager.models.DeviceInfo;

/**
 * Created by Shreya Jagarlamudi on 22/09/15.
 */
public class DialogActivity extends Activity {

    private String message;
    private Button btnOK;
    private TextView txtMessage;
    private Uri defaultRingtoneUri;
    private Ringtone defaultRingtone;
    private DeviceInfo deviceInfo;
    private String type;
    private String TAG = "DialogActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);
        btnOK = (Button) findViewById(R.id.btnOK);
        txtMessage = (TextView) findViewById(R.id.txtMessage);
        deviceInfo = new DeviceInfo(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {

            if (extras.containsKey("message")) {
                message = extras.getString("message");
            }

            type = extras.getString("type");

            if ("ring".equalsIgnoreCase(type)) {
                startAlarm();
            }
        }

        startAlarm();
        txtMessage.setText(message);

        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("ring".equalsIgnoreCase(type)) {
                    stopAlarm();
                    DialogActivity.this.finish();
                } else {
                    DialogActivity.this.finish();
                }
            }
        });
    }

    @TargetApi(21)
    private void startAlarm() {
        AudioManager am;
        am= (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);

        //For Normal mode
        am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE);
        defaultRingtone = RingtoneManager.getRingtone(this, defaultRingtoneUri);

        if (deviceInfo.getSdkVersion() >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes attributes = new AudioAttributes.Builder().
                    setUsage(AudioAttributes.USAGE_NOTIFICATION).
                    setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).
                    build();
            defaultRingtone.setAudioAttributes(attributes);
            defaultRingtone.play();
        } else {
            defaultRingtone.setStreamType(AudioManager.STREAM_NOTIFICATION);
            defaultRingtone.play();
        }
    }

    private void stopAlarm() {

        if (defaultRingtone != null && defaultRingtone.isPlaying()) {
            defaultRingtone.stop();
        }
    }
}
