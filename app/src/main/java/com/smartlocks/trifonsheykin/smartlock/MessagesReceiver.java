package com.smartlocks.trifonsheykin.smartlock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MessagesReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Toast.makeText(context, "\n\n"+intent.getStringExtra("status")+"\n\n",Toast.LENGTH_LONG).show();
        //System.out.println(intent.getStringExtra("status"));


    }
}
