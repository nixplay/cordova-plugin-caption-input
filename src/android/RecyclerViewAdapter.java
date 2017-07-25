package com.creedon.cordova.plugin.captioninput;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ProgressBarDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.util.ArrayList;
import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewHolders> implements RecyclerViewHolders.RecyclerViewHoldersListener {
    private static final String TAG = RecyclerViewAdapter.class.getSimpleName();

    @Override
    public Context getContext() {
        return this.context;
    }

    public interface RecyclerViewAdapterListener {
        boolean isPhotoSelected(int position);

        boolean isPhotoSelectionMode();
    }

    RecyclerViewAdapterListener listener;
    private List<String> itemList;
    private Context context;
    FakeR f;
    public RecyclerViewAdapter(Context context, List<String> itemList) {
        f = new FakeR(context);
        this.itemList = itemList;
        this.context = context;
        try {
            listener = (RecyclerViewAdapterListener) context;
        } catch (Exception e) {
            Log.e(TAG, "RecyclerViewAdapterListener not found");
        }
    }
    //https://stackoverflow.com/questions/30053610/best-way-to-update-data-with-a-recyclerview-adapter
    public void swap(ArrayList<String> datas, boolean bNotifyDataSetChanged){

        if (itemList != null) {
            itemList.clear();
            itemList.addAll(datas);
        }
        else {
            itemList = datas;
        }
        if(bNotifyDataSetChanged) {
            notifyDataSetChanged();
        }
    }
    @Override
    public RecyclerViewHolders onCreateViewHolder(ViewGroup parent, int viewType) {

        View layoutView = LayoutInflater.from(parent.getContext()).inflate(f.getId("layout", "layout_photocaption_card_view"), null);
        RecyclerViewHolders rcv = new RecyclerViewHolders(layoutView, this);

        return rcv;
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolders holder, int position) {

        int width = 100;
        int height = 100;
        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(itemList.get(position)))
                .setResizeOptions(new ResizeOptions(width, height))
                .build();
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setOldController(holder.simpleDraweeView.getController())
                .setImageRequest(request)
                .build();
        final ProgressBarDrawable progressBarDrawable = new ProgressBarDrawable();
        progressBarDrawable.setColor(context.getResources().getColor(f.getId("color","colorAccent")));
        progressBarDrawable.setBackgroundColor(context.getResources().getColor(f.getId("color","colorPrimaryDark")));
        progressBarDrawable
                .setRadius(5);
        final Drawable failureDrawable = context.getResources().getDrawable(f.getId("drawable","missing"));
        DrawableCompat.setTint(failureDrawable, Color.RED);
        final Drawable placeholderDrawable = context.getResources().getDrawable(f.getId("drawable","loading"));
        holder.simpleDraweeView.getHierarchy().setPlaceholderImage(placeholderDrawable, ScalingUtils.ScaleType.CENTER_INSIDE);
        holder.simpleDraweeView.getHierarchy().setFailureImage(failureDrawable, ScalingUtils.ScaleType.CENTER_INSIDE);
        holder.simpleDraweeView.getHierarchy().setProgressBarImage(progressBarDrawable, ScalingUtils.ScaleType.CENTER_INSIDE);
        if(listener != null) {
            if(listener.isPhotoSelected(position)) {
                RoundingParams roundingParams = RoundingParams.fromCornersRadius(0);
                roundingParams.setBorder(0xFF62b1e6, 3.0f);
                roundingParams.setRoundAsCircle(false);
                holder.simpleDraweeView.getHierarchy().setRoundingParams(roundingParams);
            }else{
                holder.simpleDraweeView.getHierarchy().setRoundingParams(null);
            }
        }
        holder.simpleDraweeView.setController(controller);


    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public int getItemCount() {
        return this.itemList.size();
    }
}
