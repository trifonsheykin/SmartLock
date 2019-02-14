package com.example.trifonsheykin.smartlock;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Base64;
import android.widget.Toast;

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
    private Socket nsocket; //Network Socket
    private final int port = 48910;

    private InputStream nis; //Network Input Stream
    private OutputStream nos; //Network Output Stream

    public static final String DOOR_STAT = "com.smartlock.client.DOOR_OPENED";

    byte XORcheck = 0;
    byte XORin = 0;

    byte[] secretWord = new byte[32];
    byte[] userAes = new byte[32];
    byte[] ipAddr = new byte[4];
    byte[] userId = new byte[4];
    byte userTag;

    String[] projectionKeyData = {
            LockDataContract._ID,
            LockDataContract.COLUMN_KEY_TITLE,
            LockDataContract.COLUMN_AES_KEY,
            LockDataContract.COLUMN_IP_ADDRESS,
            LockDataContract.COLUMN_DOOR_ID_STR,
            LockDataContract.COLUMN_DOOR_ID_BYTE,
            LockDataContract.COLUMN_USER_ID,
            LockDataContract.COLUMN_USER_TAG,
            LockDataContract.COLUMN_USER_START_DOOR_TIME,
            LockDataContract.COLUMN_USER_STOP_DOOR_TIME,
            LockDataContract.COLUMN_AP_SSID
    };

    String[] projectionAccessCode = {
            LockDataContract._ID,
            LockDataContract.COLUMN_AC_AES_KEY,
            LockDataContract.COLUMN_AC_SECRET_WORD,
            LockDataContract.COLUMN_AC_IP_ADDRESS,
            LockDataContract.COLUMN_AC_DOOR1_ID_STR,
            LockDataContract.COLUMN_AC_DOOR1_ID_BYTE,
            LockDataContract.COLUMN_AC_DOOR2_ID_STR,
            LockDataContract.COLUMN_AC_DOOR2_ID_BYTE,
            LockDataContract.COLUMN_AC_USER_ID,
            LockDataContract.COLUMN_AC_USER_TAG
    };
    boolean keyDataFound;
    Cursor cursorKeyData;
    Cursor cursorAccessCode;
    @Override
    protected void onHandleIntent(Intent intent) {
        String doorIdString = intent.getStringExtra("doorId");
        doorId = Base64.decode(doorIdString, Base64.DEFAULT);
        DbHelper dbHelperLock = DbHelper.getInstance(this);
        mDb = dbHelperLock.getWritableDatabase();

        cursorKeyData = mDb.query(
                LockDataContract.TABLE_NAME_KEY_DATA,
                projectionKeyData,
                LockDataContract.COLUMN_DOOR_ID_STR + "= ?",
                new String[] {doorIdString},
                null,
                null,
                LockDataContract.COLUMN_TIMESTAMP
        );
        cursorAccessCode = mDb.query(
                LockDataContract.TABLE_NAME_ACCESS_CODES,
                projectionAccessCode,
                LockDataContract.COLUMN_AC_DOOR1_ID_STR + "= ?",
                new String[] {doorIdString},
                null,
                null,
                LockDataContract.COLUMN_TIMESTAMP
        );

        if(cursorKeyData.getCount() == 0){
            if(cursorAccessCode.getCount() == 0){
                return;
            }else{//working with access code DB
                keyDataFound = false;
            }
        }else{//working with key data DB
            keyDataFound = true;
        }

        if(keyDataFound){
            cursorKeyData.moveToPosition(0);
            userAes = cursorKeyData.getBlob(cursorKeyData.getColumnIndex(LockDataContract.COLUMN_AES_KEY));
            ipAddress = cursorKeyData.getString(cursorKeyData.getColumnIndex(LockDataContract.COLUMN_IP_ADDRESS));
            userId = cursorKeyData.getBlob(cursorKeyData.getColumnIndex(LockDataContract.COLUMN_USER_ID));
            userTag = (byte)cursorKeyData.getInt(cursorKeyData.getColumnIndex(LockDataContract.COLUMN_USER_TAG));


        }else{
            cursorAccessCode.moveToPosition(0);
            secretWord = cursorAccessCode.getBlob(cursorAccessCode.getColumnIndex(LockDataContract.COLUMN_AC_SECRET_WORD));
            userAes = cursorAccessCode.getBlob(cursorAccessCode.getColumnIndex(LockDataContract.COLUMN_AC_AES_KEY));
            ipAddress = cursorAccessCode.getString(cursorAccessCode.getColumnIndex(LockDataContract.COLUMN_AC_IP_ADDRESS));
            userId = cursorAccessCode.getBlob(cursorAccessCode.getColumnIndex(LockDataContract.COLUMN_AC_USER_ID));
            userTag = (byte)cursorAccessCode.getInt(cursorAccessCode.getColumnIndex(LockDataContract.COLUMN_AC_USER_TAG));


        }

        byte[] buffer = new byte[100];
        int read = 0;
        int dialogStage = 0;
        try {
            nsocket = new Socket(ipAddress, port);
            if (nsocket.isConnected()) {
                nos = nsocket.getOutputStream();
                nis = nsocket.getInputStream();

                while(read != -1 && dialogStage < 3){
                    if(dialogStage == 0){
                        txData = makeChallengeRequestFor(userTag);
                    }else if(dialogStage == 1){
                        txData = encryptCommand(buffer);
                    }else if(dialogStage == 2){
                        saveDataToDb(replyHandle(buffer), doorIdString);
                    }
                    if (txData == null || dialogStage == 2) break;
                    nos.write(txData);
                    nos.flush();
                    dialogStage++;
                    read = nis.read(buffer, 0, 100); //This is blocking
                }



            }
        } catch (IOException e) {
            e.printStackTrace();
            Intent broadcastIntent = new Intent(DOOR_STAT);
            broadcastIntent.putExtra("status", e.getMessage());
            sendBroadcast(broadcastIntent);
        }


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
            if(keyDataFound == false){
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
        }
        return null;
    }

    private void saveDataToDb(byte[] newAes, String doorIdStr){
        if(newAes == null) return;

        if(keyDataFound){//save new aes in key data DB
            ContentValues cv = new ContentValues();
            cv.put(LockDataContract.COLUMN_AES_KEY, newAes);
            mDb.update(LockDataContract.TABLE_NAME_KEY_DATA, cv, LockDataContract.COLUMN_DOOR_ID_STR + "= ?", new String[]{doorIdStr});

        }else{//save new entry in key data and delete entry in access code
            ContentValues cvDoor1 = new ContentValues();
            cvDoor1.put(LockDataContract.COLUMN_AES_KEY, newAes);
            cvDoor1.put(LockDataContract.COLUMN_IP_ADDRESS, ipAddress);
            cvDoor1.put(LockDataContract.COLUMN_DOOR_ID_STR, doorIdStr);
            cvDoor1.put(LockDataContract.COLUMN_DOOR_ID_BYTE, doorId);
            cvDoor1.put(LockDataContract.COLUMN_USER_ID, userId);
            cvDoor1.put(LockDataContract.COLUMN_USER_TAG, userTag);
            mDb.insert(LockDataContract.TABLE_NAME_KEY_DATA, null, cvDoor1);
            mDb.delete(LockDataContract.TABLE_NAME_ACCESS_CODES, LockDataContract.COLUMN_AC_DOOR1_ID_STR + "= ?", new String[]{doorIdStr});

        }
    }

    private byte[] replyHandle(byte[] buffer){
        byte[] temp = new byte[48];
        byte[] newAes = new byte[32];

        System.arraycopy(buffer, 0, temp, 0, temp.length);
        decryptedData = decrypt(temp, initVectorRX, userAes);
        XORin = decryptedData[1];
        if (XORcheck == XORin) {
            System.arraycopy(decryptedData, 2, newAes, 0, 32);
            byte[]reply = new byte[14];
            System.arraycopy(decryptedData, 34, reply, 0, reply.length);
            return newAes;

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
