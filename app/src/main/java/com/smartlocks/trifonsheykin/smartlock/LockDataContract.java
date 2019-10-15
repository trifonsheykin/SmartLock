package com.smartlocks.trifonsheykin.smartlock;

import android.provider.BaseColumns;


public final class LockDataContract implements BaseColumns {

    public static final String TABLE_NAME_KEY_DATA = "keyData";
    public static final String COLUMN_KEY_TITLE = "keyTitle";
    public static final String COLUMN_AES_KEY = "aesKey";
    public static final String COLUMN_IP_ADDRESS = "ipAddress";
    public static final String COLUMN_DOOR_ID =  "doorIdString";
    public static final String COLUMN_DOOR_ID_BRO = "doorIdOfBro";
    public static final String COLUMN_USER_ID = "userId";
    public static final String COLUMN_USER_TAG = "userTag";
    public static final String COLUMN_USER_START_DOOR_TIME = "startDoorTime";
    public static final String COLUMN_USER_STOP_DOOR_TIME = "stopDoorTime";
    public static final String COLUMN_AP_SSID = "accessPointSsid";
    public static final String COLUMN_AC_ACTIVATED = "acActivated";
    public static final String COLUMN_AC_SECRET_WORD = "acSecretWord";

    public static final String COLUMN_TIMESTAMP = "timestamp";

}