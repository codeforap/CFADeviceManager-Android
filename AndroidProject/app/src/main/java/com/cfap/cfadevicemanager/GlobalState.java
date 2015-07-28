package com.cfap.cfadevicemanager;

import android.app.Application;

/**
 * Created by Shreya on 27/07/15.
 */

/**
 * Variables of this class are global to the entire application classes and can be accessed from anywhere using get and set
 * methods.
 */
public class GlobalState extends Application{

    private String jStr = "";

    public void setJStr(String js){
        jStr = js;
    }
    public String getjStr(){
        return jStr;
    }

}
