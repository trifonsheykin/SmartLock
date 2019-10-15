package com.smartlocks.trifonsheykin.smartlock;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class NetworkService extends IntentService {
    public NetworkService() {
        super("NetworkService");
    }
    byte[] initVectorTX = new byte[16];
    byte[] initVectorRX = new byte[16];
    byte[] txData = new byte[80];
    byte[] decryptedData = new byte[80];
    byte[] doorId;
    private SQLiteDatabase mDb;
    private static final int NEW_USER_REG = 11;
    private static final int DOOR_OPEN_MSG = 5;
    private String ipAddress;
    private String doorIdBro;
    private Socket nsocket; //Network Socket
    private final int port = 48910;

    private InputStream nis; //Network Input Stream
    private OutputStream nos; //Network Output Stream

    public static final String DOOR_STAT = "com.smartlock.client.DOOR_OPENED";//com.smartlock.client

    byte XORcheck = 0;
    byte XORin = 0;
    int active;
    byte[] secretWord = new byte[32];
    byte[] userAes = new byte[32];
    byte[] ipAddr = new byte[4];
    byte[] userId = new byte[4];
    byte userTag;

    String[] projection = {
            LockDataContract._ID,
            LockDataContract.COLUMN_KEY_TITLE,
            LockDataContract.COLUMN_AES_KEY,
            LockDataContract.COLUMN_IP_ADDRESS,
            LockDataContract.COLUMN_DOOR_ID,
            LockDataContract.COLUMN_DOOR_ID_BRO,
            LockDataContract.COLUMN_USER_ID,
            LockDataContract.COLUMN_USER_TAG,
            LockDataContract.COLUMN_USER_START_DOOR_TIME,
            LockDataContract.COLUMN_USER_STOP_DOOR_TIME,
            LockDataContract.COLUMN_AP_SSID,
            LockDataContract.COLUMN_AC_ACTIVATED,
            LockDataContract.COLUMN_AC_SECRET_WORD,
    };
    private String ssid = "No ssid";



    boolean keyDataFound;
    Cursor cursorKeyData;
    @Override
    protected void onHandleIntent(Intent intent) {

        String doorIdString = intent.getStringExtra("doorId");
        doorId = Base64.decode(doorIdString, Base64.DEFAULT);
        DbHelper dbHelperLock = DbHelper.getInstance(this);
        mDb = dbHelperLock.getWritableDatabase();
        System.out.println("service started");


        cursorKeyData = mDb.query(
                LockDataContract.TABLE_NAME_KEY_DATA,
                projection,
                LockDataContract.COLUMN_DOOR_ID + "= ?",
                new String[] {doorIdString},
                null,
                null,
                LockDataContract.COLUMN_TIMESTAMP
        );

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if(!wifiManager.isWifiEnabled()) {
            System.out.println("ERROR 01: turn ON your Wi-Fi adapter");
            Intent broadcastIntent = new Intent(this, MessagesReceiver.class);//new Intent(DOOR_STAT);
            broadcastIntent.putExtra("status", "ERROR 01: turn ON your Wi-Fi adapter");
            sendBroadcast(broadcastIntent);
            //Toast.makeText(this, "\n\nERROR 03: turn ON your Wi-Fi adapter\n\n", Toast.LENGTH_SHORT).show();
            return;
        }
        ssid = wifiInfo.getSSID();



        if(cursorKeyData.getCount() == 0){
            System.out.println("cursor count error");
            Intent broadcastIntent = new Intent(this, MessagesReceiver.class);//new Intent(DOOR_STAT);
            broadcastIntent.putExtra("status", "ERROR 02: keys not found");
            sendBroadcast(broadcastIntent);
            return;
        }

        if(cursorKeyData.getCount() > 1){
            System.out.println("too much AES keys");
            Intent broadcastIntent = new Intent(this, MessagesReceiver.class);//new Intent(DOOR_STAT);
            broadcastIntent.putExtra("status", "ERROR 03: you have conflicting keys. Delete duplicates");
            sendBroadcast(broadcastIntent);
            return;
        }

        cursorKeyData.moveToPosition(0);
        userAes = cursorKeyData.getBlob(cursorKeyData.getColumnIndex(LockDataContract.COLUMN_AES_KEY));
        ipAddress = cursorKeyData.getString(cursorKeyData.getColumnIndex(LockDataContract.COLUMN_IP_ADDRESS));
        userId = cursorKeyData.getBlob(cursorKeyData.getColumnIndex(LockDataContract.COLUMN_USER_ID));
        userTag = (byte)cursorKeyData.getInt(cursorKeyData.getColumnIndex(LockDataContract.COLUMN_USER_TAG));
        active = cursorKeyData.getInt(cursorKeyData.getColumnIndex(LockDataContract.COLUMN_AC_ACTIVATED));
        secretWord = cursorKeyData.getBlob(cursorKeyData.getColumnIndex(LockDataContract.COLUMN_AC_SECRET_WORD));
        doorIdBro = cursorKeyData.getString(cursorKeyData.getColumnIndex(LockDataContract.COLUMN_DOOR_ID_BRO));


        if(!isConecctedToDevice()){
            System.out.println("isConecctedToDevice error");
            Intent broadcastIntent = new Intent(this, MessagesReceiver.class);//new Intent(DOOR_STAT);

            broadcastIntent.putExtra("status", "ERROR 04: device's IP address ("+ipAddress+") not found");
            sendBroadcast(broadcastIntent);
            System.out.println(broadcastIntent);
            return;
        }




        byte[] buffer = new byte[100];
        int read = 0;
        int dialogStage = 0;
        try {
            System.out.println("Try to connect");
            nsocket = new Socket(ipAddress, port);
            if (nsocket.isConnected()) {
                System.out.println("Connection established");
                nos = nsocket.getOutputStream();
                nis = nsocket.getInputStream();

                while(read != -1 && dialogStage < 3){
                    if(dialogStage == 0){
                        txData = makeChallengeRequestFor(userTag);
                        System.out.println("Make challenge");
                    }else if(dialogStage == 1){
                        txData = encryptCommand(buffer);
                        System.out.println("encryptCommand");
                    }else if(dialogStage == 2){
                        saveDataToDb(replyHandle(buffer), doorIdString);
                        System.out.println("saveDataToDb");
                    }
                    if (txData == null || dialogStage == 2) break;
                    nos.write(txData);
                    nos.flush();
                    dialogStage++;
                    read = nis.read(buffer, 0, 100); //This is blocking
                    if(read == 8){
                        System.out.println("we'he got error");
                        errorHandle(buffer);
                        break;
                    }
                }



            }
        } catch (IOException e) {
            e.printStackTrace();
            Intent broadcastIntent = new Intent(this, MessagesReceiver.class);//new Intent(DOOR_STAT);
            broadcastIntent.putExtra("status", "ERROR 05: " + e.getMessage());
            sendBroadcast(broadcastIntent);
        }


    }

    private void errorHandle(byte[] buffer){

        byte[] error = new byte[8];
        System.arraycopy(buffer, 0, error, 0, 8);//userID[USER_ID_SIZE];//4
        String errorCode = new String(error);
        String out;
        if(errorCode.equals("ERROR 10"))//SEQ_START_MSG_LENGTH_ERR = 10,//input message is no valid length
            out = errorCode.concat(": SEQ_START_MSG_LENGTH_ERR\nmessage length is not valid");
        else if(errorCode.equals("ERROR 11"))// SEQ_FLOW_MSG_LENGTH_ERR = 11,//input message is no valid length
            out = errorCode.concat(": SEQ_FLOW_MSG_LENGTH_ERR\nmessage length is not valid");
        else if(errorCode.equals("ERROR 12"))//NEW_MSG_LINK_ERR = 12, //device is working with another link at the moment
            out = errorCode.concat(": NEW_MSG_LINK_ERR\nDevice is busy. Try to connect later");
        else if(errorCode.equals("ERROR 13"))//PING_XOR_ERR = 13,
            out = errorCode.concat(": PING_XOR_ERR\nCheck synchronisation");
        else if(errorCode.equals("ERROR 14"))//PING_TEXT_ERR = 14,
            out = errorCode.concat(": PING_TEXT_ERR\nNot ping command");
        else if(errorCode.equals("ERROR 20"))// SYNC_XOR_CHECK_ERR = 20,
            out = errorCode.concat(": SYNC_XOR_CHECK_ERR\nSync process error");
        else if(errorCode.equals("ERROR 21"))// SYNC_XOR_CMD_ERR = 21,
            out = errorCode.concat(": SYNC_XOR_CMD_ERR\nSync process error");
        else if(errorCode.equals("ERROR 22"))//SYNC_FLOW_CMD_ERR = 22,
            out = errorCode.concat(": SYNC_FLOW_CMD_ERR\nSync process error");
        else if(errorCode.equals("ERROR 23"))//MSG_TWO_TYPE_ERR = 23,
            out = errorCode.concat(": MSG_TWO_TYPE_ERR\nYour access code is not valid");
        else if(errorCode.equals("ERROR 24"))//USER_TAG_ERR = 24,
            out = errorCode.concat(": USER_TAG_ERR\nCheck your access code");
        else if(errorCode.equals("ERROR 30"))//DOOR_OPEN_XOR_ERR = 30,
            out = errorCode.concat(": DOOR_OPEN_XOR_ERR\nCheck your access code");
        else if(errorCode.equals("ERROR 31"))//DOOR_OPEN_USER_ID_ERR = 31,
            out = errorCode.concat(": DOOR_OPEN_USER_ID_ERR\nCheck your access code");
        else if(errorCode.equals("ERROR 32"))//DOOR_OPEN_DOOR_ID_ERR = 32,
            out = errorCode.concat(": DOOR_OPEN_DOOR_ID_ERR\nCheck the door ID");
        else if(errorCode.equals("ERROR 33"))//DOOR_OPEN_ASSESS_TIME_ERR = 33,
            out = errorCode.concat(": DOOR_OPEN_ASSESS_TIME_ERR\nCheck your access time");
        else if(errorCode.equals("ERROR 40"))//PASS_ACTION_XOR_ERR = 40,
            out = errorCode.concat(": PASS_ACTION_XOR_ERR\nCheck synchronisation");
        else if(errorCode.equals("ERROR 41"))//PASS_ACTION_USER_ID_ERR = 41,
            out = errorCode.concat(": PASS_ACTION_USER_ID_ERR\nCheck synchronisation");
        else if(errorCode.equals("ERROR 42"))//PASS_ACTION_ADMIN_TAG_ERR = 42,
            out = errorCode.concat(": PASS_ACTION_ADMIN_TAG_ERR\nCheck synchronisation");
        else if(errorCode.equals("ERROR 43"))//PASS_ACTION_PLAY_PASS_ERR = 43,
            out = errorCode.concat(": PASS_ACTION_PLAY_PASS_ERR\nCheck your pass code");
        else if(errorCode.equals("ERROR 50"))//NEW_USER_ADM_KEY_ERR = 50,
            out = errorCode.concat(": NEW_USER_ADM_KEY_ERR\nCheck your access code");
        else if(errorCode.equals("ERROR 51"))//NEW_USER_XOR_CHECK_ERR = 51,
            out = errorCode.concat(": NEW_USER_XOR_CHECK_ERR\nCheck your access code");
        else if(errorCode.equals("ERROR 52"))//NEW_USER_ID_EMPTY_ERR = 52,
            out = errorCode.concat(": NEW_USER_ID_EMPTY_ERR\nCheck your access code");
        else if(errorCode.equals("ERROR 53"))//NEW_USER_ID_KEY_ERR = 53,
            out = errorCode.concat(": NEW_USER_ID_KEY_ERR\nCheck your access code");
        else if(errorCode.equals("ERROR 60"))//SECRET_CHECK_AES_ERR = 60,
            out = errorCode.concat(": SECRET_CHECK_AES_ERR\nCheck your access code");
        else if(errorCode.equals("ERROR 61"))//SECRET_ACCESS_TIME_ERR = 61,
            out = errorCode.concat(": SECRET_ACCESS_TIME_ERR\nCheck your access time");
        else if(errorCode.equals("ERROR 62"))//SECRET_ACCESS_RTC_ERR = 62,
            out = errorCode.concat(": SECRET_ACCESS_RTC_ERR\nCheck your access time");
        else if(errorCode.equals("ERROR 63"))//SECRET_USER_ID_ERR = 63
            out = errorCode.concat(": SECRET_USER_ID_ERR\nCheck your access code");
        else
            out = errorCode.concat(": description not found");

        System.out.println(out);
        Intent broadcastIntent = new Intent(this, MessagesReceiver.class);//new Intent(DOOR_STAT);
        broadcastIntent.putExtra("status", out);
        sendBroadcast(broadcastIntent);

    }

    public boolean isConecctedToDevice() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 " + ipAddress);
            int     exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch (IOException e)          { e.printStackTrace(); }
        catch (InterruptedException e) { e.printStackTrace(); }
        return false;
    }

    private byte[] makeChallengeRequestFor(byte userTag){
        SecureRandom random = new SecureRandom();
        byte[] nonce= new byte[16];
        random.nextBytes(nonce);
        nonce[0] = userTag;
        XORcheck = XORcalc(nonce);
        System.arraycopy(nonce, 0, initVectorRX, 0, 16);//SAVE it to receive message
        return nonce;
    }
    private byte[] encryptCommand(byte[] buffer){
        System.arraycopy(buffer, 0, initVectorTX, 0, 16);
        byte[] temp = new byte[16];
        System.arraycopy(buffer, 0, temp, 0, temp.length);
        decryptedData = decrypt(temp, initVectorRX, userAes);
        System.arraycopy(decryptedData, 0, temp, 0, 16);
        XORin = decryptedData[0];
        if (XORcheck == XORin){
            if(active == 0){
                byte[] plaintext = new byte[48];
                plaintext[0] = NEW_USER_REG;//MESSAGE TYPE: NEW_USER_REG 11
                plaintext[1] = XORcalc(temp);//XOR
                System.arraycopy(userId, 0, plaintext, 2, 4);//userID[USER_ID_SIZE];//4
                System.arraycopy(secretWord, 0, plaintext, 6, 32);//userID[USER_ID_SIZE];//4
                System.arraycopy(doorId, 0, plaintext, 38, 4);//userID[USER_ID_SIZE];//4
                XORcheck = XORcalc(plaintext);
                txData = encrypt(plaintext, initVectorTX, userAes);
                System.arraycopy(txData, 32, initVectorRX, 0, 16);//SAVE it to receive message
            }else{
                byte[] plaintext = new byte[16];
                plaintext[0] = DOOR_OPEN_MSG;//MESSAGE TYPE: DOOR_OPEN_MSG = 5;
                plaintext[1] = XORcalc(temp);//XOR
                System.arraycopy(userId, 0, plaintext, 2, 4);//userID[USER_ID_SIZE];//4
                System.arraycopy(doorId, 0, plaintext, 6, 4);//userID[USER_ID_SIZE];//4
                XORcheck = XORcalc(plaintext);
                txData = encrypt(plaintext, initVectorTX, userAes);
                System.arraycopy(txData, 0, initVectorRX, 0, 16);//SAVE it to receive message
            }

            return txData;
        }else{
            Intent broadcastIntent = new Intent(this, MessagesReceiver.class);//new Intent(DOOR_STAT);
            broadcastIntent.putExtra("status", "ERROR 06: XOR check error.\nYour access code is not valid");
            sendBroadcast(broadcastIntent);
        }
        return null;
    }

    private void saveDataToDb(byte[] newAes, String doorIdStr){
        if(newAes == null){
            Intent broadcastIntent = new Intent(this, MessagesReceiver.class);//new Intent(DOOR_STAT);
            broadcastIntent.putExtra("status", "No new AES key");
            sendBroadcast(broadcastIntent);
            return;
        }

        ContentValues cv = new ContentValues();
        cv.put(LockDataContract.COLUMN_AES_KEY, newAes);
        cv.put(LockDataContract.COLUMN_AP_SSID, ssid);
        if(active == 0) {//NEW KEW REGISTERED
            cv.put(LockDataContract.COLUMN_AC_ACTIVATED, 1);
            cv.put(LockDataContract.COLUMN_AC_SECRET_WORD, (byte[]) null);
        }
        mDb.update(LockDataContract.TABLE_NAME_KEY_DATA, cv, LockDataContract.COLUMN_DOOR_ID + "= ?", new String[]{doorIdStr});
        mDb.update(LockDataContract.TABLE_NAME_KEY_DATA, cv, LockDataContract.COLUMN_DOOR_ID + "= ?", new String[]{doorIdBro});



    }

    private byte[] replyHandle(byte[] buffer){
        byte[] temp = new byte[48];
        byte[] newAes = new byte[32];
        System.out.println("replyHandle");

        System.arraycopy(buffer, 0, temp, 0, temp.length);
        decryptedData = decrypt(temp, initVectorRX, userAes);
        XORin = decryptedData[1];
        if (XORcheck == XORin) {
            System.out.println("XORcheck == XORin");
            System.arraycopy(decryptedData, 2, newAes, 0, 32);
            byte[]reply = new byte[14];
            System.arraycopy(decryptedData, 34, reply, 0, reply.length);
            Intent broadcastIntent = new Intent(this, MessagesReceiver.class);//new Intent(DOOR_STAT);
            broadcastIntent.putExtra("status", new String(reply));
            sendBroadcast(broadcastIntent);
            return newAes;

        }else{
            Intent broadcastIntent = new Intent(this, MessagesReceiver.class);//new Intent(DOOR_STAT);
            broadcastIntent.putExtra("status", "ERROR 06: XOR check error.\nYour access code is not valid");
            sendBroadcast(broadcastIntent);
        }
        return null;
    }


    private byte XORcalc(byte[] input){
        byte output = input[0];
        for(int i=1; i<input.length; i++) output = (byte) (output ^ input[i]);
        return output;
    }

    public byte[] decrypt(byte[] data, byte[] initVector, byte[] key) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector);
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec k = new SecretKeySpec(key, "AES_256");
            cipher.init(Cipher.DECRYPT_MODE, k, iv);
            byte[] plainText = cipher.doFinal(data);
            return plainText;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] encrypt(byte[] data, byte[] initVector, byte[] key) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector);
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec k = new SecretKeySpec(key, "AES_256");
            cipher.init(Cipher.ENCRYPT_MODE, k, iv);
            byte[] cipherByte = cipher.doFinal(data);
            return cipherByte;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



}
