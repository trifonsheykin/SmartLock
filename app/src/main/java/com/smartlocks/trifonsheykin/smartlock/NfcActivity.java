package com.smartlocks.trifonsheykin.smartlock;

import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Parcelable;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

public class NfcActivity extends AppCompatActivity {
    NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);
        String doorId = null;
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(100);
        }

            nfcAdapter = NfcAdapter.getDefaultAdapter(this);
            Intent intent = getIntent();
            String action = intent.getAction();
            if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) && intent.hasExtra(NfcAdapter.EXTRA_TAG)){
                NdefMessage ndefMessage = getNdefMessageFromIntent(intent);
                doorId = getStringFromNdefRecord(getNdefRecordFromNdefMessage(ndefMessage));
            }else{
                Toast.makeText(this, "NFC intent error", Toast.LENGTH_SHORT).show();
            }

            Intent serviceIntent = new Intent(NfcActivity.this, NetworkService.class);
            serviceIntent.putExtra("doorId", doorId);
            startService(serviceIntent);

        finish();
    }

    private NdefMessage getNdefMessageFromIntent(Intent intent){
        Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if(parcelables != null && parcelables.length > 0){
            return (NdefMessage)parcelables[0];
        }else{
            Toast.makeText(this, "No NDEF messages found", Toast.LENGTH_LONG).show();
        }
        return null;
    }

    private NdefRecord getNdefRecordFromNdefMessage(NdefMessage ndefMessage) {
        NdefRecord[] ndefRecords = ndefMessage.getRecords();

        if(ndefRecords != null && ndefRecords.length>0){
            return ndefRecords[0];
        }else{
            Toast.makeText(this, "No NDEF records found", Toast.LENGTH_LONG).show();
        }
        return null;
    }

    public String getStringFromNdefRecord (NdefRecord ndefRecord){
        String tagContent = null;
        try{
            byte[] payload = ndefRecord.getPayload();
            String textEncoding = ((payload[0] & 128) != 0) ? "UTF-16" : "UTF-8";
            int languageSize = payload[0] & 0063;
            tagContent = new String(payload, languageSize + 1, payload.length - languageSize - 1, textEncoding);
        }catch (UnsupportedEncodingException e){
            Toast.makeText(this, "NFC-tag error 4", Toast.LENGTH_LONG).show();
        }
        return tagContent;
    }
}
