package com.example.trifonsheykin.smartlock;

import android.provider.BaseColumns;


public final class LockDataContract implements BaseColumns {

    public static final String TABLE_NAME_KEY_DATA = "keyData";
    public static final String COLUMN_KEY_TITLE = "keyTitle";
    public static final String COLUMN_AES_KEY = "aesKey";
    public static final String COLUMN_IP_ADDRESS = "ipAddress";
    public static final String COLUMN_DOOR_ID_STR =  "doorIdString";
    public static final String COLUMN_DOOR_ID_BYTE = "doorIdByte";
    public static final String COLUMN_USER_ID = "userId";
    public static final String COLUMN_USER_TAG = "userTag";
    public static final String COLUMN_USER_START_DOOR_TIME = "startDoor1Time";
    public static final String COLUMN_USER_STOP_DOOR_TIME = "stopDoor1Time";
    public static final String COLUMN_AP_SSID = "accessPointSsid";


    public static final String TABLE_NAME_ACCESS_CODES = "accessCodes";
    public static final String COLUMN_AC_AES_KEY = "acAesKey";
    public static final String COLUMN_AC_SECRET_WORD = "acSecretWord";
    public static final String COLUMN_AC_IP_ADDRESS = "acIpAddress";
    public static final String COLUMN_AC_DOOR1_ID_STR =  "door1IdString";
    public static final String COLUMN_AC_DOOR1_ID_BYTE = "door1IdByte";
    public static final String COLUMN_AC_DOOR2_ID_STR =  "door2IdString";
    public static final String COLUMN_AC_DOOR2_ID_BYTE = "door2IdByte";
    public static final String COLUMN_AC_USER_ID = "acUserId";
    public static final String COLUMN_AC_USER_TAG = "acUserTag";


    public static final String COLUMN_TIMESTAMP = "timestamp";

}