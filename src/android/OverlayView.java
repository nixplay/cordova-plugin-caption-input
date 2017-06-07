package com.creedon.cordova.plugin.captioninput;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import com.creedon.androidphotobrowser.common.views.ImageOverlayView;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.List;

/**
 * _   _ _______   ________ _       _____   __
 * | \ | |_   _\ \ / /| ___ \ |     / _ \ \ / /
 * |  \| | | |  \ V / | |_/ / |    / /_\ \ V /
 * | . ` | | |  /   \ |  __/| |    |  _  |\ /
 * | |\  |_| |_/ /^\ \| |   | |____| | | || |
 * \_| \_/\___/\/   \/\_|   \_____/\_| |_/\_/
 * <p>
 * Created by jameskong on 6/6/2017.
 */

class OverlayView extends ImageOverlayView implements RecyclerItemClickListener.OnItemClickListener {
    public interface OverlayVieListener extends ImageOverlayVieListener{

        List<String> getItemlist();
    }
    FakeR f;
    private RecyclerViewAdapter recyclerViewAdapter;

    public OverlayView(Context context) {
        super(context);
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected View init() {

        f = new FakeR(getContext());
        View view = inflate(getContext(), f.getId("layout", "layout_imageoverlay"), this);
        etDescription = (MaterialEditText) view.findViewById(f.getId("id", "etDescription"));
        RecyclerView recyclerView = (RecyclerView) view.findViewById(f.getId("id", "recycleview"));
        recyclerViewAdapter = new RecyclerViewAdapter(getContext(), ((OverlayVieListener)listener).getItemlist());
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getContext(), recyclerView, this));
        etDescription.setVisibility(VISIBLE);
        return view;

    }

    @Override
    public void onItemClick(View view, int position) {

    }

    @Override
    public void onItemLongClick(View view, int position) {

    }
}
