package com.example.trifonsheykin.smartlock;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

public class ViewHolderKey extends RecyclerView.ViewHolder {
    public TextView keyTitle;
    public TextView keyUser;

    public ViewHolderKey(View itemView, View.OnClickListener clickListener) {
        super(itemView);

        keyTitle = itemView.findViewById(R.id.tvKeyTitle);
        keyUser = itemView.findViewById(R.id.tvKeyUser);
        itemView.setOnClickListener(clickListener);

    }
}
