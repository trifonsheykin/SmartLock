package com.example.trifonsheykin.smartlock;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity{

    private Button bEnterAccessCode;
    private Button bOpenDoor;
    private TextView tvStatus;
    private EditText etAccessCode;
    private String accessCode;
    private SQLiteDatabase mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        bEnterAccessCode = findViewById(R.id.b_enter_access_code);
        bOpenDoor = findViewById(R.id.b_open_door);
        etAccessCode = findViewById(R.id.et_access_code);
        tvStatus = findViewById(R.id.tv_status);

        DbHelper dbHelperLock = DbHelper.getInstance(this);
        mDb = dbHelperLock.getWritableDatabase();


        bEnterAccessCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               //create new entry to database
                accessCode = etAccessCode.getText().toString();
                if(accessCode.isEmpty()){
                    tvStatus.setText("ERROR: No access code found");
                }else{
                    createNewAccessCodeInDb(accessCode);
                }
            }
        });


        bOpenDoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NetworkService.class);
                intent.putExtra("doorId", "MTIzNA==\n");
                startService(intent);

            }
        });

    }



    private void createNewAccessCodeInDb(String accessCode){
        byte[] accessBytes = Base64.decode(accessCode, Base64.DEFAULT);
        if(accessBytes.length != 57) {
            tvStatus.setText("ERROR: access code length != 57");
            return;
        }
        byte[] secretWord = new byte[32];
        byte[] userAes = new byte[32];
        byte[] ipAddr = new byte[4];
        byte[] door1Id = new byte[4];
        byte[] door2Id = new byte[4];
        byte[] userId = new byte[4];
        byte userTag;
        System.arraycopy(accessBytes, 0, userAes, 0, 16);
        System.arraycopy(accessBytes, 0, userAes, 16, 16);
        System.arraycopy(accessBytes, 0, userId, 0, 4);
        System.arraycopy(accessBytes, 16, secretWord, 0, 32);
        System.arraycopy(accessBytes, 48, ipAddr, 0, 4);
        System.arraycopy(accessBytes, 52, door1Id, 0, 4);
        System.arraycopy(accessBytes, 52, door2Id, 0, 4);
        door2Id[3]++;
        String door1IdStr = Base64.encodeToString(door1Id, Base64.DEFAULT);
        String door2IdStr = Base64.encodeToString(door2Id, Base64.DEFAULT);
        userTag = accessBytes[56];
        String ipAddress = ipByteToStr(ipAddr);

        ContentValues cv = new ContentValues();
        cv.put(LockDataContract.COLUMN_AC_AES_KEY, userAes);
        cv.put(LockDataContract.COLUMN_AC_SECRET_WORD, secretWord);
        cv.put(LockDataContract.COLUMN_AC_IP_ADDRESS, ipAddress);
        cv.put(LockDataContract.COLUMN_AC_DOOR1_ID_BYTE, door1Id);
        cv.put(LockDataContract.COLUMN_AC_DOOR2_ID_BYTE, door2Id);
        cv.put(LockDataContract.COLUMN_AC_DOOR1_ID_STR, door1IdStr);
        cv.put(LockDataContract.COLUMN_AC_DOOR2_ID_STR, door2IdStr);
        cv.put(LockDataContract.COLUMN_AC_USER_ID, userId);
        cv.put(LockDataContract.COLUMN_AC_USER_TAG, userTag);

        mDb.delete(LockDataContract.TABLE_NAME_ACCESS_CODES, LockDataContract.COLUMN_AC_DOOR1_ID_STR + "= ?", new String[]{door1IdStr});
        mDb.insert(LockDataContract.TABLE_NAME_ACCESS_CODES, null, cv);
    }

    private String ipByteToStr(byte[] ip){
        String output = new String();
        for(int i = 0; i < ip.length; i++){
            output = output + Integer.toString((int)ip[i] & 0xFF);
            if(i!=3) output = output + ".";
        }
        return output;
    }

}

