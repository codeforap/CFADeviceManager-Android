package com.cfap.cfadevicemanager;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by Shreya Jagarlamudi on 03/09/15.
 */
public class ISTDateTime {

    Date IST;

    public ISTDateTime(){
        setIST(getCurrIndianDate());
    }

    private void setIST(Date date){
        IST = date;
    }

    public Date getIST(){
        return IST;
    }

    /**
     * Gets the current indian date no matter what the device time or time zone is set to
     * @return
     */
    private Date getCurrIndianDate() {
        // TODO Auto-generated method stub
        Date indianDate = null;

        SimpleDateFormat currformatter = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a");
        GregorianCalendar cal = new GregorianCalendar();
        String dstring = currformatter.format(cal.getTime());
        // Log.e("Tel Frag", "gettime in indian date: "+cal.getTime());
        Date rdate = null;
        try {
            rdate = currformatter.parse(dstring);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        TimeZone tz = TimeZone.getDefault();
        // From TimeZone current
        Log.i("Tel Frag", "getindiandate: TimeZone : " + tz.getID() + " - " + tz.getDisplayName());
        Log.i("Tel Frag", "getindiandate: TimeZone : " + tz);
        Log.i("Tel Frag", "getindiandate: Date : " + currformatter.format(rdate));

        // To TimeZone Asia/Calcutta
        SimpleDateFormat sdfIndia = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a");
        TimeZone tzInIndia = TimeZone.getTimeZone("Asia/Calcutta");
        sdfIndia.setTimeZone(tzInIndia);

        String sDateInIndia = sdfIndia.format(rdate); // Convert to String first
        Date dateInIndia = null;
        try {
            dateInIndia = currformatter.parse(sDateInIndia);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Log.i("Tel Frag", "getindiandate: \nTimeZone : " + tzInIndia.getID() +
                " - " + tzInIndia.getDisplayName());
        Log.i("Tel Frag", "getindiandate: TimeZone : " + tzInIndia);
        Log.i("Tel Frag", "getindiandate: Date (String) : " + sDateInIndia);
        Log.i("Tel Frag", "getindiandate: Date (Object) : " + currformatter.format(dateInIndia));

        return dateInIndia;
    }
}
