package com.example.trifonsheykin.smartlock;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

public class ViewHolderKey extends RecyclerView.ViewHolder {
    public TextView keyAccessTime;
    public TextView keyTitleState;
    public TextView keySsid;

    public ViewHolderKey(View itemView, View.OnClickListener clickListener) {
        super(itemView);

        keyAccessTime = itemView.findViewById(R.id.tv_access_time);
        keyTitleState = itemView.findViewById(R.id.tv_key_title_stat);
        keySsid = itemView.findViewById(R.id.tv_ssid);
        itemView.setOnClickListener(clickListener);

    }
}
