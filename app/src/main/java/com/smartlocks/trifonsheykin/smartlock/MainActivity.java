package com.smartlocks.trifonsheykin.smartlock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class MainActivity extends AppCompatActivity {

    private String accessCode;
    private SQLiteDatabase mDb;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor spEditor;
    private boolean qrScanner;
    RecyclerView recyclerViewKey;
    private Cursor cursor;
    private DataAdapterKey dataAdapterKey;
    private final int QR_SCAN = 0;
    private final int NEW_KEY = 1;
    private final int EDIT_KEY = 2;

    String[] projection = {
            LockDataContract._ID,
            LockDataContract.COLUMN_DOOR_ID
    };


    private final View.OnClickListener keyItemClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    long id = (long) view.getTag();
                    Intent intent= new Intent(MainActivity.this, KeyInfoActivity.class);
                    intent.putExtra("rowId", id);
                    startActivityForResult(intent, EDIT_KEY);////request code edit lock

                } };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerViewKey = findViewById(R.id.keyRecycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerViewKey.setLayoutManager(linearLayoutManager);
        DbHelper dbHelperKey = DbHelper.getInstance(this);
        mDb = dbHelperKey.getWritableDatabase();
        cursor = getAllKeys();

        dataAdapterKey = new DataAdapterKey(this, cursor, keyItemClickListener);
        recyclerViewKey.setAdapter(dataAdapterKey);




        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                //do nothing, we only care about swiping
                return false;
            }
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                long id = (long) viewHolder.itemView.getTag();
                cursor = mDb.query(
                        LockDataContract.TABLE_NAME_KEY_DATA,
                        projection,
                        LockDataContract._ID + "= ?",
                        new String[] {String.valueOf(id)},
                        null,
                        null,
                        LockDataContract.COLUMN_TIMESTAMP);
                cursor.moveToPosition(0);
                String doorId = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_DOOR_ID));
                Intent serviceIntent = new Intent(MainActivity.this, NetworkService.class);
                serviceIntent.putExtra("doorId", doorId);
                startService(serviceIntent);
                System.out.println(serviceIntent);
                dataAdapterKey.swapCursor(getAllKeys());

            }
        }).attachToRecyclerView(recyclerViewKey);


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        qrScanner = sharedPreferences.getBoolean("qrScannerEnable", false);
        if(qrScanner){
            Intent intent = new Intent(MainActivity.this, QrReadActivity.class);
            startActivityForResult(intent, QR_SCAN);
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AccessCodeActivity.class);
                startActivityForResult(intent, NEW_KEY);
            }
        });


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == QR_SCAN){
                Intent serviceIntent = new Intent(MainActivity.this, NetworkService.class);
                serviceIntent.putExtra("doorId", data.getStringExtra("result"));
                startService(serviceIntent);
                //finish();

            }else if(requestCode == NEW_KEY){
                dataAdapterKey.swapCursor(getAllKeys());
                recyclerViewKey.smoothScrollToPosition(dataAdapterKey.getItemCount());


            }else if(requestCode == EDIT_KEY){
                dataAdapterKey.swapCursor(getAllKeys());

            }



        }








    }

    private Cursor getAllKeys() {
        return mDb.query(
                LockDataContract.TABLE_NAME_KEY_DATA,
                null,
                null,
                null,
                null,
                null,
                LockDataContract.COLUMN_TIMESTAMP
        );
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
            startActivityForResult(intent, QR_SCAN);
            return true;
        }else if (item.getItemId() == R.id.nav_settings){
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;

        }
        return false;
    }

}
