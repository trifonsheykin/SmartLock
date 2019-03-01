package com.example.trifonsheykin.smartlock;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

    private static DbHelper sInstance;

    // The database name
    private static final String DATABASE_NAME = "SmartLockUserData.db";

    // If you change the database schema, you must increment the database version
    private static final int DATABASE_VERSION = 1;

    public static synchronized DbHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DbHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        final String SQL_CREATE_USERDATA_TABLE = "CREATE TABLE " + LockDataContract.TABLE_NAME_KEY_DATA //
                + " ("
                + LockDataContract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + LockDataContract.COLUMN_KEY_TITLE + " TEXT, "
                + LockDataContract.COLUMN_AES_KEY + " BLOB, "
                + LockDataContract.COLUMN_IP_ADDRESS + " TEXT, "
                + LockDataContract.COLUMN_DOOR_ID + " TEXT, "
                + LockDataContract.COLUMN_DOOR_ID_BRO + " TEXT, "
                + LockDataContract.COLUMN_USER_ID + " BLOB, "
                + LockDataContract.COLUMN_USER_TAG + " INTEGER, "
                + LockDataContract.COLUMN_USER_START_DOOR_TIME + " BLOB, "
                + LockDataContract.COLUMN_USER_STOP_DOOR_TIME + " BLOB, "
                + LockDataContract.COLUMN_AP_SSID + " TEXT, "
                + LockDataContract.COLUMN_AC_ACTIVATED + " INTEGER, "
                + LockDataContract.COLUMN_AC_SECRET_WORD + " BLOB, "
                + LockDataContract.COLUMN_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + "); ";

        db.execSQL(SQL_CREATE_USERDATA_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + LockDataContract.TABLE_NAME_KEY_DATA);
        onCreate(db);

    }

}