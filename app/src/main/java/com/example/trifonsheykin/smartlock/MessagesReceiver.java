package com.example.trifonsheykin.smartlock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MessagesReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Toast.makeText(context, "Error: " + intent.getStringExtra("status"),
                Toast.LENGTH_LONG).show();


    }
}
