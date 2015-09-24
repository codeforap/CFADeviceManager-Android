package com.cfap.cfadevicemanager.services;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by Shreya Jagarlamudi on 07/09/15.
 */
public class ForegroundActivityReceiver extends BroadcastReceiver{

    private String TAG = "ForegroundActivityReceiver";
    final int PROCESS_STATE_TOP = 2;
    private Context ctx;

    @Override
    public void onReceive(Context context, Intent intent) {
        ctx = context;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
                foregroundTaskPostLollipop();
            } else {
                foregroundTaskPre();
            }
    }

    public void foregroundTaskPostLollipop(){
      //  Log.e(TAG, "in/post Lollipop");
        final PackageManager pm = ctx.getPackageManager();
        Field field = null;
        try {
            field = ActivityManager.RunningAppProcessInfo.class.getDeclaredField("processState");
        } catch (Exception ignored) {
        }
        ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();

        if(appProcesses!=null){
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                try {
                    PackageInfo pkgInfo = pm.getPackageInfo(appProcess.pkgList[0],
                            PackageManager.GET_ACTIVITIES);

                    if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                            && appProcess.importanceReasonCode == ActivityManager.RunningAppProcessInfo.REASON_UNKNOWN){
                        Integer state = null;
                        try {
                            state = field.getInt(appProcess);
                        } catch (Exception e) {
                        }
                        if (state != null && state == PROCESS_STATE_TOP) {
                            if(appProcess.processName.equals("com.sec.android.app.launcher") ||
                                    appProcess.processName.equals("com.cfap.cfadevicemanager") ||
                                    (pkgInfo.applicationInfo.flags &
                                            ApplicationInfo.FLAG_SYSTEM)!=0){
                               // Toast.makeText(ctx, "No applications are foreground at this time!", 3).show();
                            }else{
                                Toast.makeText(ctx, "You do not have access to this application", 3).show();
                                Intent i = new Intent();
                                i.setAction(Intent.ACTION_MAIN);
                                i.addCategory(Intent.CATEGORY_HOME);
                                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                ctx.startActivity(i);
                            }
                            break;
                        }
                    }

                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void foregroundTaskPre(){
      //  Log.e(TAG, "in pre Lollipop")
        ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningTaskInfo> taskInfo = activityManager.getRunningTasks(1);
        final ComponentName componentName = taskInfo.get(0).topActivity;
        final String[] activePackages = new String[1];
        activePackages[0] = componentName.getPackageName();
        Log.e(TAG, "is system package? "+isSystemApp(activePackages[0]));
        if(activePackages[0].equals("com.sec.android.app.launcher") || activePackages[0].equals("com.cfap.cfadevicemanager") ||
                isSystemApp(activePackages[0])==true){
           // Toast.makeText(ctx, "No applications are foreground at this time!", 3).show();
        }else{
            Toast.makeText(ctx, "You do not have access to this application", 3).show();
            Intent i = new Intent();
            i.setAction(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        }
    }

    public boolean isSystemApp(String packagename){
        final PackageManager pm = ctx.getPackageManager();
        List<PackageInfo> packs = pm.getInstalledPackages(0);
        for(int i=0; i < packs.size(); i++) {
            PackageInfo p = packs.get(i);
            ApplicationInfo a = p.applicationInfo;
            if(p.packageName.equals(packagename) && (a.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                return true;
            }
        }
        return false;
    }
}
