package com.cfap.cfadevicemanager;

import android.app.Application;
import android.widget.EditText;

/**
 * Created by Shreya on 27/07/15.
 */

/**
 * Variables of this class are global to the entire application classes and can be accessed from anywhere using get and set
 * methods.
 */
public class GlobalState extends Application{

    private String jStr = "";
    private String connStatus="";


    public void setJStr(String js){
        jStr = js;
    }
    public String getjStr(){
        return jStr;
    }

    public void setConnStatus(String s){
        connStatus = s;
    }
    public String getConnStatus(){
        return connStatus;
    }


}
