package com.creedon.cordova.plugin.captioninput;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.facebook.drawee.view.SimpleDraweeView;

public class RecyclerViewHolders extends RecyclerView.ViewHolder implements View.OnClickListener {
    private FakeR f = null;
    SimpleDraweeView simpleDraweeView;
    public ImageView videoIcon;

    public interface RecyclerViewHoldersListener{
        Context getContext();
    }
    private RecyclerViewHoldersListener listener;
    public RecyclerViewHolders(View itemView, RecyclerViewHoldersListener listener) {
        super(itemView);
        this.listener = listener;
        itemView.setOnClickListener(this);
        if(this.listener != null){
            f = new FakeR(this.listener.getContext());
        }
        if(f !=null) {
            simpleDraweeView = (SimpleDraweeView) itemView.findViewById(f.getId("id","image"));
            videoIcon = (ImageView) itemView.findViewById(f.getId("id","iv_video"));
        }

    }

    @Override
    public void onClick(View view) {
//            Toast.makeText(view.getContext(), "Clicked Country Position = " + getPosition(), Toast.LENGTH_SHORT).show();
    }
}
