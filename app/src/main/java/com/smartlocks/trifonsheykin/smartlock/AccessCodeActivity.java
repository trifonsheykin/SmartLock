package com.smartlocks.trifonsheykin.smartlock;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

public class AccessCodeActivity extends AppCompatActivity {

    private ImageButton ibQrScanner;
    private Button bClear;
    private Button bPaste;
    private Button bSave;
    private EditText etAccessCode;
    private TextView tvKeyData;
    private SQLiteDatabase mDb;
    int accessCodeType;
    private final int ERROR_LOCK = 0;
    private final int MULTIPLE_LOCKS = 2;
    private final int ONE_LOCK = 1;
    byte[] secretWord32 = new byte[32];
    byte[] secretWord16 = new byte[16];
    byte[] userAes = new byte[32];
    byte[] ipAddr = new byte[4];
    byte[] door1Id = new byte[4];
    byte[] door2Id = new byte[4];
    byte[] userId = new byte[4];
    byte userTag;
    String door1IdStr;
    String door2IdStr;
    String ipAddress;
    byte[] doorStopTime = new byte[5];
    byte[] door1StartTime = new byte[5];
    byte[] door1StopTime = new byte[5];
    byte[] door2StartTime = new byte[5];
    byte[] door2StopTime = new byte[5];
    private ClipboardManager clipboard;
    ArrayList<byte[]> ipAddrDoorId = new ArrayList<byte[]>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_code);

        ibQrScanner = findViewById(R.id.ib_qr_code);
        bClear = findViewById(R.id.b_clear);
        bPaste = findViewById(R.id.b_paste_from_clipboard);
        bSave = findViewById(R.id.b_save_access_code);
        etAccessCode = findViewById(R.id.et_access_code);
        tvKeyData = findViewById(R.id.tv_key_data);
        tvKeyData.setText("");

        DbHelper dbHelperLock = DbHelper.getInstance(this);
        mDb = dbHelperLock.getWritableDatabase();
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        bSave.setEnabled(false);
        bClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etAccessCode.setText("");
            }
        });
        bPaste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (clipboard.hasPrimaryClip()) {
                    ClipDescription description = clipboard.getPrimaryClipDescription();
                    ClipData data = clipboard.getPrimaryClip();
                    if (data != null && description != null
                            && (description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                            || description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML))){
                        etAccessCode.setText(String.valueOf(data.getItemAt(0).getText()));

                    }

                }

            }
        });

        bSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(accessCodeType != ERROR_LOCK)
                    saveKeyToDb();
                setResult(RESULT_OK);
                ClipData clipData = ClipData.newPlainText("", "");
                clipboard.setPrimaryClip(clipData);
                finish();
            }
        });
        ibQrScanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AccessCodeActivity.this, QrReadActivity.class);
                startActivityForResult(intent, 0);
            }
        });


        etAccessCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(etAccessCode.getText().toString().length() == 0){
                    tvKeyData.setText("");
                    bSave.setEnabled(false);
                }else{
                    tvKeyData.setText(decodeAccessString(etAccessCode.getText().toString()));
                    if(tvKeyData.getText().toString().contains("time"))bSave.setEnabled(true);
                    else bSave.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }


    private String decodeAccessString(String accessCode){
        String out;
        byte[] accessBytes;
        try{
            accessBytes = Base64.decode(accessCode, Base64.NO_WRAP);
        }catch (Exception e){
            return "Incorrect access code (DECODE)";
        }
        if(!xorOk(accessBytes)) {
            return "Incorrect access code (XOR)";
        }


        if(accessBytes.length == 78) {
            accessCodeType = ONE_LOCK;
            System.arraycopy(accessBytes, 0,  userAes,    0, 16);
            System.arraycopy(accessBytes, 0,  userAes,   16, 16);
            System.arraycopy(accessBytes, 0,  userId,     0,  4);
            System.arraycopy(accessBytes, 16, secretWord32, 0, 32);
            System.arraycopy(accessBytes, 48, ipAddr,     0,  4);
            System.arraycopy(accessBytes, 52, door1Id,    0,  4);
            System.arraycopy(accessBytes, 52, door2Id,    0,  4);
            door2Id[3]++;
            door1IdStr = Base64.encodeToString(door1Id, Base64.NO_WRAP);
            door2IdStr = Base64.encodeToString(door2Id, Base64.NO_WRAP);
            userTag = accessBytes[56];
            ipAddress = ipByteToStr(ipAddr);

            System.arraycopy(accessBytes, 57, door1StartTime, 0, 5);
            System.arraycopy(accessBytes, 62, door1StopTime, 0, 5);
            System.arraycopy(accessBytes, 67, door2StartTime, 0, 5);
            System.arraycopy(accessBytes, 72, door2StopTime, 0, 5);

            out =       "Door 1 access time:"+
                              "\nFrom: " + accessTimeByteToStr(door1StartTime) +
                           "\n     To: " + accessTimeByteToStr(door1StopTime) +
                    "\n\nDoor 2 access time:"+
                              "\nFrom: " + accessTimeByteToStr(door2StartTime) +
                           "\n     To: " + accessTimeByteToStr(door2StopTime);


        }else if((accessBytes.length - 47) % 8 == 0){
            accessCodeType = MULTIPLE_LOCKS;
            System.arraycopy(accessBytes, 0,  userAes,    0, 16);
            System.arraycopy(accessBytes, 0,  userAes,   16, 16);
            System.arraycopy(accessBytes, 0,  userId,     0,  4);
            System.arraycopy(accessBytes, 16, secretWord16, 0, 16);
            System.arraycopy(accessBytes, 33, doorStopTime, 0, 5);
            System.arraycopy(accessBytes, 38, ipAddr,     0,  4);
            System.arraycopy(accessBytes, 42, door1Id,    0,  4);
            System.arraycopy(accessBytes, 42, door2Id,    0,  4);
            door2Id[3]++;
            door1IdStr = Base64.encodeToString(door1Id, Base64.NO_WRAP);
            door2IdStr = Base64.encodeToString(door2Id, Base64.NO_WRAP);
            userTag = accessBytes[32];
            ipAddress = ipByteToStr(ipAddr);

            byte[] ipId;
            for(int i = 46; i < accessBytes.length - 1; i += 8){
                ipId = new byte[8];
                System.arraycopy(accessBytes, i, ipId,0,8);
                ipAddrDoorId.add(ipId);
            }

            out = "Expiration time: " + accessTimeByteToStr(doorStopTime) +
                     "\nIP address: " + ipAddress;


        }else{
            accessCodeType = ERROR_LOCK;
            out = "Incorrect access code (LENGTH)";
        }

        return out;

    }
    private String accessTimeByteToStr(byte[] date){
        String hour, minute, day, month, year;

        year =  new String("" + (date[0] >> 4) + (date[0] & 0x0F));
        month =  new String("" + (date[1] >> 4) + (date[1] & 0x0F));
        day =  new String("" + (date[2] >> 4) + (date[2] & 0x0F));
        hour =  new String("" + (date[3] >> 4) + (date[3] & 0x0F));
        minute =  new String("" + (date[4] >> 4) + (date[4] & 0x0F));

        return new String(hour + ":" + minute + " " + day + "." + month + "." + year);
    }



    private boolean xorOk(byte[] input){
        byte[] toXor = new byte[input.length - 1];
        System.arraycopy(input, 0, toXor, 0, toXor.length);
        if(XORcalc(toXor) == input[input.length - 1])
            return true;
        return false;
    }

    private void saveKeyToDb(){
        ContentValues cv1;
        ContentValues cv2;
        if(accessCodeType == ONE_LOCK){
            cv1 = new ContentValues();
            cv1.put(LockDataContract.COLUMN_KEY_TITLE, "Key 1");
            cv1.put(LockDataContract.COLUMN_AES_KEY, userAes);
            cv1.put(LockDataContract.COLUMN_IP_ADDRESS, ipAddress);
            cv1.put(LockDataContract.COLUMN_DOOR_ID_BRO, door2IdStr);
            cv1.put(LockDataContract.COLUMN_DOOR_ID, door1IdStr);
            cv1.put(LockDataContract.COLUMN_USER_ID, userId);
            cv1.put(LockDataContract.COLUMN_USER_TAG, userTag);
            cv1.put(LockDataContract.COLUMN_USER_START_DOOR_TIME, door1StartTime);
            cv1.put(LockDataContract.COLUMN_USER_STOP_DOOR_TIME, door1StopTime);
            cv1.put(LockDataContract.COLUMN_AP_SSID, "Not defined");
            cv1.put(LockDataContract.COLUMN_AC_ACTIVATED, 0);
            cv1.put(LockDataContract.COLUMN_AC_SECRET_WORD, secretWord32);
            mDb.insert(LockDataContract.TABLE_NAME_KEY_DATA, null, cv1);

            cv2 = new ContentValues();
            cv2.put(LockDataContract.COLUMN_KEY_TITLE, "Key 2");
            cv2.put(LockDataContract.COLUMN_AES_KEY, userAes);
            cv2.put(LockDataContract.COLUMN_IP_ADDRESS, ipAddress);
            cv2.put(LockDataContract.COLUMN_DOOR_ID_BRO, door1IdStr);
            cv2.put(LockDataContract.COLUMN_DOOR_ID, door2IdStr);
            cv2.put(LockDataContract.COLUMN_USER_ID, userId);
            cv2.put(LockDataContract.COLUMN_USER_TAG, userTag);
            cv2.put(LockDataContract.COLUMN_USER_START_DOOR_TIME, door2StartTime);
            cv2.put(LockDataContract.COLUMN_USER_STOP_DOOR_TIME, door2StopTime);
            cv2.put(LockDataContract.COLUMN_AP_SSID, "Not defined");
            cv2.put(LockDataContract.COLUMN_AC_ACTIVATED, 0);
            cv2.put(LockDataContract.COLUMN_AC_SECRET_WORD, secretWord32);
            mDb.insert(LockDataContract.TABLE_NAME_KEY_DATA, null, cv2);

        }else if(accessCodeType == MULTIPLE_LOCKS){
            byte[] doorStartTime = getCurrentTime();
            cv1 = new ContentValues();
            cv1.put(LockDataContract.COLUMN_KEY_TITLE, "Key 1");
            cv1.put(LockDataContract.COLUMN_AES_KEY, userAes);
            cv1.put(LockDataContract.COLUMN_IP_ADDRESS, ipAddress);
            cv1.put(LockDataContract.COLUMN_DOOR_ID_BRO, door2IdStr);
            cv1.put(LockDataContract.COLUMN_DOOR_ID, door1IdStr);
            cv1.put(LockDataContract.COLUMN_USER_ID, userId);
            cv1.put(LockDataContract.COLUMN_USER_TAG, userTag);
            cv1.put(LockDataContract.COLUMN_USER_START_DOOR_TIME, doorStartTime);
            cv1.put(LockDataContract.COLUMN_USER_STOP_DOOR_TIME, doorStopTime);
            cv1.put(LockDataContract.COLUMN_AP_SSID, "Not defined");
            cv1.put(LockDataContract.COLUMN_AC_ACTIVATED, 2);//2 - this key used to receive super AES key, 3 - is working with existing super key
            cv1.put(LockDataContract.COLUMN_AC_SECRET_WORD, secretWord16);
            mDb.insert(LockDataContract.TABLE_NAME_KEY_DATA, null, cv1);

            cv2 = new ContentValues();
            cv2.put(LockDataContract.COLUMN_KEY_TITLE, "Key 2");
            cv2.put(LockDataContract.COLUMN_AES_KEY, userAes);
            cv2.put(LockDataContract.COLUMN_IP_ADDRESS, ipAddress);
            cv2.put(LockDataContract.COLUMN_DOOR_ID_BRO, door1IdStr);
            cv2.put(LockDataContract.COLUMN_DOOR_ID, door2IdStr);
            cv2.put(LockDataContract.COLUMN_USER_ID, userId);
            cv2.put(LockDataContract.COLUMN_USER_TAG, userTag);
            cv2.put(LockDataContract.COLUMN_USER_START_DOOR_TIME, doorStartTime);
            cv2.put(LockDataContract.COLUMN_USER_STOP_DOOR_TIME, doorStopTime);
            cv2.put(LockDataContract.COLUMN_AP_SSID, "Not defined");
            cv2.put(LockDataContract.COLUMN_AC_ACTIVATED, 2);
            cv2.put(LockDataContract.COLUMN_AC_SECRET_WORD, secretWord16);
            mDb.insert(LockDataContract.TABLE_NAME_KEY_DATA, null, cv2);

            int i = 3;
            for(byte[] ipid : ipAddrDoorId){
                System.arraycopy(ipid, 0, ipAddr,     0,  4);
                System.arraycopy(ipid, 4, door1Id,    0,  4);
                System.arraycopy(ipid, 4, door2Id,    0,  4);
                door2Id[3]++;
                door1IdStr = Base64.encodeToString(door1Id, Base64.NO_WRAP);
                door2IdStr = Base64.encodeToString(door2Id, Base64.NO_WRAP);
                userTag = 10;
                ipAddress = ipByteToStr(ipAddr);

                cv1 = new ContentValues();
                cv1.put(LockDataContract.COLUMN_KEY_TITLE, "Key " + (i++));
                cv1.put(LockDataContract.COLUMN_AES_KEY, userAes);
                cv1.put(LockDataContract.COLUMN_IP_ADDRESS, ipAddress);
                cv1.put(LockDataContract.COLUMN_DOOR_ID_BRO, door2IdStr);
                cv1.put(LockDataContract.COLUMN_DOOR_ID, door1IdStr);
                cv1.put(LockDataContract.COLUMN_USER_ID, userId);
                cv1.put(LockDataContract.COLUMN_USER_TAG, userTag);
                cv1.put(LockDataContract.COLUMN_USER_START_DOOR_TIME, doorStartTime);
                cv1.put(LockDataContract.COLUMN_USER_STOP_DOOR_TIME, doorStopTime);
                cv1.put(LockDataContract.COLUMN_AP_SSID, "Not defined");
                cv1.put(LockDataContract.COLUMN_AC_ACTIVATED, 3);//2 - this key used to receive super AES key, 3 - is working with existing super key
                cv1.put(LockDataContract.COLUMN_AC_SECRET_WORD, secretWord16);
                mDb.insert(LockDataContract.TABLE_NAME_KEY_DATA, null, cv1);

                cv2 = new ContentValues();
                cv2.put(LockDataContract.COLUMN_KEY_TITLE, "Key " + (i++));
                cv2.put(LockDataContract.COLUMN_AES_KEY, userAes);
                cv2.put(LockDataContract.COLUMN_IP_ADDRESS, ipAddress);
                cv2.put(LockDataContract.COLUMN_DOOR_ID_BRO, door1IdStr);
                cv2.put(LockDataContract.COLUMN_DOOR_ID, door2IdStr);
                cv2.put(LockDataContract.COLUMN_USER_ID, userId);
                cv2.put(LockDataContract.COLUMN_USER_TAG, userTag);
                cv2.put(LockDataContract.COLUMN_USER_START_DOOR_TIME, doorStartTime);
                cv2.put(LockDataContract.COLUMN_USER_STOP_DOOR_TIME, doorStopTime);
                cv2.put(LockDataContract.COLUMN_AP_SSID, "Not defined");
                cv2.put(LockDataContract.COLUMN_AC_ACTIVATED, 3);
                cv2.put(LockDataContract.COLUMN_AC_SECRET_WORD, secretWord16);
                mDb.insert(LockDataContract.TABLE_NAME_KEY_DATA, null, cv2);

            }
        }
    }

    private byte[] getCurrentTime(){
        Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH)+1;
        int year = c.get(Calendar.YEAR);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute  = c.get(Calendar.MINUTE);

        byte[] output = new byte[5];
        int tempX_, temp_X;

        temp_X = year % 10;
        tempX_ = (year / 10 % 10) << 4;
        output[0] = (byte) (tempX_ | temp_X);

        temp_X = month % 10;
        tempX_ = (month / 10 % 10) << 4;
        output[1] = (byte) (tempX_ | temp_X);

        temp_X = day % 10;
        tempX_ = (day / 10 % 10) << 4;
        output[2] = (byte) (tempX_ | temp_X);

        temp_X = hour % 10;
        tempX_ = (hour/ 10 % 10) << 4;
        output[3] = (byte) (tempX_ | temp_X);

        temp_X = minute % 10;
        tempX_ = (minute / 10 % 10) << 4;
        output[4] = (byte) (tempX_ | temp_X);

        return output;

    }

    public byte XORcalc(byte[] input){
        byte output = input[0];
        for(int i=1; i<input.length; i++) output = (byte) (output ^ input[i]);
        return output;
    }



    private String ipByteToStr(byte[] ip){
        String output = new String();
        for(int i = 0; i < ip.length; i++){
            output = output + Integer.toString((int)ip[i] & 0xFF);
            if(i!=3) output = output + ".";
        }
        return output;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == 0){
            String result = data.getStringExtra("result");
            etAccessCode.setText(result);//(new String(Base64.decode(result, Base64.DEFAULT)));

        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }
}
