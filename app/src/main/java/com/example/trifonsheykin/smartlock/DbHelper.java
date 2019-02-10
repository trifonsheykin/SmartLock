package com.example.trifonsheykin.smartlock;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

    private static DbHelper sInstance;

    // The database name
    private static final String DATABASE_NAME = "slUserData.db";

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
                + LockDataContract.COLUMN_DOOR_ID_STR + " TEXT, "
                + LockDataContract.COLUMN_DOOR_ID_BYTE + " BLOB, "
                + LockDataContract.COLUMN_USER_ID + " BLOB, "
                + LockDataContract.COLUMN_USER_TAG + " INTEGER, "
                + LockDataContract.COLUMN_USER_START_DOOR_TIME + " BLOB, "
                + LockDataContract.COLUMN_USER_STOP_DOOR_TIME + " BLOB, "
                + LockDataContract.COLUMN_AP_SSID + " TEXT, "
                + LockDataContract.COLUMN_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + "); ";

        final String SQL_CREATE_ACCESSCODES_TABLE = "CREATE TABLE " + LockDataContract.TABLE_NAME_ACCESS_CODES//
                + " ("
                + LockDataContract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + LockDataContract.COLUMN_AC_AES_KEY + " BLOB, "
                + LockDataContract.COLUMN_AC_SECRET_WORD + " BLOB, "
                + LockDataContract.COLUMN_AC_IP_ADDRESS + " TEXT, "
                + LockDataContract.COLUMN_AC_DOOR1_ID_STR + " TEXT, "
                + LockDataContract.COLUMN_AC_DOOR1_ID_BYTE + " BLOB, "
                + LockDataContract.COLUMN_AC_DOOR2_ID_STR + " TEXT, "
                + LockDataContract.COLUMN_AC_DOOR2_ID_BYTE + " BLOB, "
                + LockDataContract.COLUMN_AC_USER_ID + " BLOB, "
                + LockDataContract.COLUMN_AC_USER_TAG + " INTEGER, "
                + LockDataContract.COLUMN_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + "); ";

        db.execSQL(SQL_CREATE_USERDATA_TABLE);
        db.execSQL(SQL_CREATE_ACCESSCODES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For now simply drop the table and create a new one. This means if you change the
        // DATABASE_VERSION the table will be dropped.
        // In a production app, this method might be modified to ALTER the table
        // instead of dropping it, so that existing data is not deleted.
        db.execSQL("DROP TABLE IF EXISTS " + LockDataContract.TABLE_NAME_KEY_DATA);
        db.execSQL("DROP TABLE IF EXISTS " + LockDataContract.TABLE_NAME_KEY_DATA);
        onCreate(db);

    }

}