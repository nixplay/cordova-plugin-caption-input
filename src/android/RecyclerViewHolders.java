package com.creedon.cordova.plugin.captioninput;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.facebook.drawee.view.SimpleDraweeView;

public class RecyclerViewHolders extends RecyclerView.ViewHolder implements View.OnClickListener {
    SimpleDraweeView simpleDraweeView;


    public RecyclerViewHolders(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);
//        simpleDraweeView = (SimpleDraweeView) itemView.findViewById(com.creedon.androidphotobrowser.R.id.image);

    }

    @Override
    public void onClick(View view) {
//            Toast.makeText(view.getContext(), "Clicked Country Position = " + getPosition(), Toast.LENGTH_SHORT).show();
    }
}