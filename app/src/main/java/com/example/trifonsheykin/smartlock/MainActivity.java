package com.example.trifonsheykin.smartlock;

import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private String accessCode;
    private SQLiteDatabase mDb;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor spEditor;
    private boolean qrScanner;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DbHelper dbHelperLock = DbHelper.getInstance(this);
        mDb = dbHelperLock.getWritableDatabase();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        qrScanner = sharedPreferences.getBoolean("qrScannerEnable", false);
        if(qrScanner){
            Intent intent = new Intent(MainActivity.this, QrReadActivity.class);
            startActivityForResult(intent, 0);
        }







        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AccessCodeActivity.class);
                startActivityForResult(intent, 1);
            }
        });





    }


    public String readFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            android.content.ClipDescription description = clipboard.getPrimaryClipDescription();
            android.content.ClipData data = clipboard.getPrimaryClip();
            if (data != null && description != null
                    && (description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                    || description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)))
                return String.valueOf(data.getItemAt(0).getText());
        }
        return null;
    }



    private void createNewAccessCodeInDb(String accessCode){
        byte[] accessBytes = Base64.decode(accessCode, Base64.DEFAULT);
        if(accessBytes.length != 57) {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == 0){
            Toast.makeText(getApplicationContext(),data.getStringExtra("result"),Toast.LENGTH_SHORT).show();


        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.nav_qr_scan) {
            Intent intent = new Intent(MainActivity.this, QrReadActivity.class);
            startActivityForResult(intent, 0);
            return true;
        }else if (item.getItemId() == R.id.nav_settings){
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;

        }
        return false;
    }

}
