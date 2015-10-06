package com.cfap.cfadevicemanager.dbmodels;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Created by Shreya Jagarlamudi on 19/08/15.
 */
public class DatabaseHelper extends SQLiteOpenHelper{

    private static Context myContext;
    private SQLiteDatabase myDatabase;
    private DatabaseHelper dbHelp;
    private static DatabaseHelper mInstance;
    private String TAG = "DatabaseHelper";
    static int DB_VERSION = 1;
    static final String DB_NAME = "Cfadm.sqlite";
    static String DB_PATH;

    // Table Names
    private static final String GENERAL = "general";
    private static final String TASKS = "tasks";
    private static final String DATATABLE = "data_table";
    private static final String APP_INSTALLATION = "app_installation";

    // general column names
    private static final String COL_imei = "imei";
    private static final String COL_registered = "registered";
    private static final String COL_model = "model";
    private static final String COL_version = "version";

    // tasks column names
    private static final String COL_datewtime = "datewtime";
    private static final String COL_type = "type";
    private static final String COL_json = "json";
    private static final String COL_status = "status";

    //app_installation column names
    private static final String COL_APP_NAME = "app_name";
    private static final String COL_APP_PACKAGE = "app_package";
    private static final String COL_install_status = "install_status";
    private static final String COL_app_url = "app_url";
    private static final String COL_app_type = "app_type";
    private static final String COL_app_install_name = "app_name";

    // data_table column names
    private static final String COL_appname = "appname";
    private static final String COL_date = "date";
    private static final String COL_dailyusageRec = "dailyusageRec";
    private static final String COL_dailywifiusageRec = "dailywifiusageRec";
    private static final String COL_dailydatausageRec = "dailydatausageRec";
    private static final String COL_monthlyusageRec = "monthlyusageRec";
    private static final String COL_monthlywifiusageRec = "monthlywifiusageRec";
    private static final String COL_monthlydatausageRec = "monthlydatausageRec";
    private static final String COL_dailyusageSent = "dailyusageSent";
    private static final String COL_dailywifiusageSent = "dailywifiusageSent";
    private static final String COL_dailydatausageSent = "dailydatausageSent";
    private static final String COL_monthlyusageSent = "monthlyusageSent";
    private static final String COL_monthlywifiusageSent = "monthlywifiusageSent";
    private static final String COL_monthlydatausageSent = "monthlydatausageSent";

    public static synchronized DatabaseHelper getInstance(Context context) {
        if(mInstance == null) {
            Log.e("DbHelper", "inside null instance of Dbhelper!");
            mInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return mInstance;
    }

    public DatabaseHelper(Context applicationContext) {
        // TODO Auto-generated constructor stub
        super(applicationContext, DB_NAME, null, DB_VERSION);
        DatabaseHelper.myContext = applicationContext;
        DB_PATH = myContext.getApplicationInfo().dataDir+"/databases/";
        Log.e("DbHelper", "db path: " + DB_PATH);
    }

    public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    /**
     * Creates a empty database on the system and rewrites it with your own database.
     * */
    public void createDataBase() throws IOException {

        boolean dbExist = checkDataBase();

        if(dbExist){
            //do nothing - database already exist
            Log.e("DbHelper", "DB already exists!");

        }else{

            //By calling this method and empty database will be created into the default system path
            //of your application so we are gonna be able to overwrite that database with our database.
            this.getReadableDatabase();

            try {

                copyDataBase();

            } catch (IOException e) {

                throw new Error("Error copying database");

            }
        }

    }

    private boolean checkDataBase(){
        File dbFile = new File(DB_PATH+DB_NAME);
        return dbFile.exists();
    }

    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transfering bytestream.
     * */
    private void copyDataBase() throws IOException{

        //Open your local db as the input stream
        InputStream myInput = myContext.getAssets().open(DB_NAME);

        // Path to the just created empty db
        String outFileName = DB_PATH + DB_NAME;

        //Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(outFileName);

        //transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer))>0){
            myOutput.write(buffer, 0, length);
        }

        //Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();

    }

    public void openDataBase() throws SQLException {
        //Open the database
        String myPath = DB_PATH + DB_NAME;
        myDatabase= SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);
    }

    public void open(){
        dbHelp = new DatabaseHelper(myContext);
        myDatabase = dbHelp.getWritableDatabase();
    }

    @Override
    public synchronized void close() {

        if(myDatabase != null)
            myDatabase.close();

        super.close();
    }

    public void insertTask(String date, String type, String json, String status){
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_datewtime, date);
        cv.put(COL_type, type);
        cv.put(COL_json, json);
        cv.put(COL_status, status);
        db.insert(TASKS, null, cv);
        Log.e(TAG, "inserted new task");
    }

    public void updateTaskStatus(String json, String status){
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_status, status);
        String where = "json=?";
        String[] values = {json};
        db.update(TASKS, cv, where, values);
    }

    public int getNumberOfPendingTasks(){
        int n = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        n = (int) DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM tasks WHERE status='pending'", null);
        return n;
    }

    public ArrayList<String> getPendingJsons(){
        ArrayList<String> pendingArray = new ArrayList<String>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT json FROM tasks WHERE status='pending'", null);
        int getIndex = cursor.getColumnIndex("json");
        cursor.moveToFirst();
        while(!cursor.isAfterLast()){
            pendingArray.add(cursor.getString(getIndex));
            cursor.moveToNext();
        }
        cursor.close();
        Log.e(TAG, "pending tasks array: " + pendingArray);
        return pendingArray;
    }

    public ArrayList<String> getSentJsons(){
        ArrayList<String> pendingArray = new ArrayList<String>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT json FROM tasks WHERE status='sent'", null);
        int getIndex = cursor.getColumnIndex("json");
        cursor.moveToFirst();
        while(!cursor.isAfterLast()){
            pendingArray.add(cursor.getString(getIndex));
            cursor.moveToNext();
        }
        cursor.close();
        Log.e(TAG, "sent tasks array: " + pendingArray);
        return pendingArray;
    }

    public void eraseSentDataFromDb(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.rawQuery("DELETE FROM tasks WHERE status='sent'", null);
        db.close();
    }

    public void insertImei(String imei){
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_imei, imei);
        db.insert(GENERAL, null, cv);
    }

    public String getImei(){
        String myImei = "testimei";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT imei FROM general", null);
        int getImeiIndex = cursor.getColumnIndex("imei");
        cursor.moveToFirst();
        while(!cursor.isAfterLast()){
           myImei =  cursor.getString(getImeiIndex);
           cursor.moveToNext();

        }
        return myImei;
    }

    public void updateModel(String imei, String model){
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_model, model);
        String where = "model=?";
        String[] values = {model};
        db.update(GENERAL, cv, where, values);
    }

    public String getModel(){
        String model = "testmodel";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT model FROM general", null);
        int getIndex = cursor.getColumnIndex("model");
        cursor.moveToFirst();
        while(!cursor.isAfterLast()){
            model =  cursor.getString(getIndex);
            cursor.moveToNext();
        }
        return model;
    }

    public void updateVersion(String imei, String version){
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_version, version);
        String where = "version=?";
        String[] values = {version};
        db.update(GENERAL, cv, where, values);
    }

    public String getVersion(){
        String version = "testversion";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT version FROM general", null);
        int getIndex = cursor.getColumnIndex("version");
        cursor.moveToFirst();
        while(!cursor.isAfterLast()){
            version =  cursor.getString(getIndex);
            cursor.moveToNext();
        }
        return version;
    }

    public void insertRegistered(int reg, String imei){
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_registered, reg);
        String where = "imei=?";
        String[] values = {imei};
        db.update(GENERAL, cv, where, values);
    }

    public int getRegistered(String imei){
        int myreg = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT registered FROM general WHERE imei='"+imei+"'", null);
        int getRegIndex = cursor.getColumnIndex("registered");
        cursor.moveToFirst();
        while(!cursor.isAfterLast()){
            myreg =  cursor.getInt(getRegIndex);
            cursor.moveToNext();

        }
        return myreg;
    }

    public void insertAppInstallReq(String app_package, String status, String app_url, String app_type, String app_name){
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_APP_PACKAGE, app_package);
        cv.put(COL_install_status, status);
        cv.put(COL_app_url, app_url);
        cv.put(COL_app_type, app_type);
        cv.put(COL_app_install_name, app_name);
        db.update(APP_INSTALLATION, cv, null, null);
    }

    public void updateAppInstallStatus(String app_package, String status){
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_install_status, status);
        String where = "app_package=?";
        String[] values = {app_package};
        db.update(GENERAL, cv, where, values);
    }

    public String getAppInstallStatus(String app_package){
        String status = "";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT status FROM app_installation WHERE app_package='"+app_package+"'", null);
        int getIndex = cursor.getColumnIndex("status");
        cursor.moveToFirst();
        while(!cursor.isAfterLast()){
            status = cursor.getString(getIndex);
            cursor.moveToNext();
        }
        return status;
    }

    public void deleteAppInstallEntry(){

    }
}
