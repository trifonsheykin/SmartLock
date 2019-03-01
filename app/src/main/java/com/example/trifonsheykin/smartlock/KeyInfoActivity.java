package com.example.trifonsheykin.smartlock;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class KeyInfoActivity extends AppCompatActivity {

    private EditText etKeyTitle;
    private TextView tvKeyData;
    private Button bSave;
    private Button bDelete;

    private SQLiteDatabase mDb;
    private long id;
    private Cursor cursor;
    String[] projection = {
            LockDataContract._ID,
            LockDataContract.COLUMN_KEY_TITLE,
            LockDataContract.COLUMN_IP_ADDRESS,
            LockDataContract.COLUMN_DOOR_ID,
            LockDataContract.COLUMN_USER_START_DOOR_TIME,
            LockDataContract.COLUMN_USER_STOP_DOOR_TIME,
            LockDataContract.COLUMN_AP_SSID,
            LockDataContract.COLUMN_AC_ACTIVATED,
            LockDataContract.COLUMN_TIMESTAMP
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_info);

        etKeyTitle = findViewById(R.id.et_key_title);
        tvKeyData = findViewById(R.id.tv_key_info);
        bSave = findViewById(R.id.b_save_key);
        bDelete = findViewById(R.id.b_delete_key);



        // Create a DB helper (this will create the DB if run for the first time)
        DbHelper dbHelperLock = DbHelper.getInstance(this);
        mDb = dbHelperLock.getWritableDatabase();

        Intent intent = getIntent();
        id = intent.getLongExtra("rowId", -1);//intent.putExtra("rowId", id);
        if(id == -1) tvKeyData.setText("ERROR ROW ID");
        else{
            cursor = mDb.query(
                    LockDataContract.TABLE_NAME_KEY_DATA,
                    projection,
                    LockDataContract._ID + "= ?",
                    new String[] {String.valueOf(id)},
                    null,
                    null,
                    LockDataContract.COLUMN_TIMESTAMP);
            cursor.moveToPosition(0);

            String keyTitle = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_KEY_TITLE));
            String ipAddress = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_IP_ADDRESS));
            String doorId = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_DOOR_ID));
            byte[] startTime = cursor.getBlob(cursor.getColumnIndex(LockDataContract.COLUMN_USER_START_DOOR_TIME));
            byte[] stopTime = cursor.getBlob(cursor.getColumnIndex(LockDataContract.COLUMN_USER_STOP_DOOR_TIME));
            String apSsid = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_AP_SSID));
            int active = cursor.getInt(cursor.getColumnIndex(LockDataContract.COLUMN_AC_ACTIVATED));
            long timestamp = cursor.getLong(cursor.getColumnIndex(LockDataContract.COLUMN_TIMESTAMP));

            etKeyTitle.setText(keyTitle);
            tvKeyData.setText(  "Access point SSID: "  + apSsid +
                            "\n\nLock IP-address: "    + ipAddress +
                            "\n\nDoor ID: "            + doorId +
                            "\n\nStart time: "         + accessTimeByteToStr(startTime) +
                            "\n\nStop time: "          + accessTimeByteToStr(stopTime) +
                            "\n\nAccess code status: " + ((active == 0) ? "not activated":"activated") +
                            "\n\nTimestamp: "          + timestamp);
        }


        bDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeKey(id);
                setResult(RESULT_OK);
                finish();
            }
        });

        bSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentValues cv = new ContentValues();
                cv.put(LockDataContract.COLUMN_KEY_TITLE, etKeyTitle.getText().toString());
                mDb.update(LockDataContract.TABLE_NAME_KEY_DATA, cv, LockDataContract._ID + "= ?", new String[]{String.valueOf(id)});
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private boolean removeKey(long id) {
        return mDb.delete(LockDataContract.TABLE_NAME_KEY_DATA, LockDataContract._ID + "=" + id, null) > 0;
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
