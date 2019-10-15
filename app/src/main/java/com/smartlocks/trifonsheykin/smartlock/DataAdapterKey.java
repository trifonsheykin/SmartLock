package com.smartlocks.trifonsheykin.smartlock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DataAdapterKey extends RecyclerView.Adapter<ViewHolderKey>  {

    private LayoutInflater inflater;
    private Cursor cursor;
    private View.OnClickListener clickListener;

    public DataAdapterKey(Context context, Cursor cursor, View.OnClickListener clickListener) {
        this.cursor = cursor;
        this.clickListener = clickListener;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolderKey onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_key, parent, false);

        return new ViewHolderKey(view, clickListener);
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onBindViewHolder(ViewHolderKey holder, int position) {

        if (!cursor.moveToPosition(position))
            return; // bail if returned null


        // Update the view holder with the information needed to display
        byte[] accessTimeStart = cursor.getBlob(cursor.getColumnIndex(LockDataContract.COLUMN_USER_START_DOOR_TIME));
        byte[] accessTimeStop = cursor.getBlob(cursor.getColumnIndex(LockDataContract.COLUMN_USER_STOP_DOOR_TIME));
        String accessTime = "from: " + accessTimeByteToStr(accessTimeStart) + "\n     to: " + accessTimeByteToStr(accessTimeStop);
        int keyStatus = cursor.getInt(cursor.getColumnIndex(LockDataContract.COLUMN_AC_ACTIVATED));
        String keyTitle = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_KEY_TITLE));
        String ssid = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_AP_SSID));
        long id = cursor.getLong(cursor.getColumnIndex(LockDataContract._ID));


        holder.keyAccessTime.setText(accessTime);
        if(keyStatus == 0) {
            holder.keyTitleState.setBackgroundColor(Color.LTGRAY);
            holder.keyTitleState.setText(keyTitle + ": not activated");
        } else {
            holder.keyTitleState.setBackgroundColor(Color.WHITE);
            holder.keyTitleState.setText(keyTitle + ": is active");
        }
        holder.keySsid.setText("Lock SSID: " + ssid);
        holder.itemView.setTag(id);
    }

    private String accessTimeByteToStr(byte[] date){
        String hour, minute, day, month, year;

        year =  new String("" + (date[0] >> 4) + (date[0] & 0x0F));
        month =  new String("" + (date[1] >> 4) + (date[1] & 0x0F));
        day =  new String("" + (date[2] >> 4) + (date[2] & 0x0F));
        hour =  new String("" + (date[3] >> 4) + (date[3] & 0x0F));
        minute =  new String("" + (date[4] >> 4) + (date[4] & 0x0F));

        return new String(hour + ":" + minute + " " + day + "/" + month + "/" + year);
    }

    @Override
    public int getItemCount()  {
        int count = cursor.getCount();
        return count;
    }



    public void swapCursor(Cursor newCursor) {
        // Always close the previous mCursor first
        if (cursor != null) cursor.close();
        cursor = newCursor;
        if (newCursor != null) {
            // Force the RecyclerView to refresh
            this.notifyDataSetChanged();

        }
    }
}
