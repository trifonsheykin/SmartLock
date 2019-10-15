package com.smartlocks.trifonsheykin.smartlock;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class SettingsActivity extends AppCompatActivity {


    private CheckBox cbQrScannerEnable;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor spEditor;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        cbQrScannerEnable = findViewById(R.id.cb_enable_qr);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        spEditor = sharedPreferences.edit();

        cbQrScannerEnable.setChecked(sharedPreferences.getBoolean("qrScannerEnable", false));
        cbQrScannerEnable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                spEditor.putBoolean("qrScannerEnable", isChecked);
                spEditor.commit();
            }
        });



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
