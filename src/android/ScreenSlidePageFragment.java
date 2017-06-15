package com.creedon.cordova.plugin.captioninput;


import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.creedon.cordova.plugin.captioninput.zoomable.DoubleTapGestureListener;
import com.creedon.cordova.plugin.captioninput.zoomable.ZoomableDraweeView;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ProgressBarDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

/**
 * A simple {@link Fragment} subclass.
 */
public class ScreenSlidePageFragment extends Fragment {

    private static final String ARG_URL = "ARG_URL";
    private String url;
    private FakeR fakeR;
    private Context context;

    public interface ScreenSlidePageFragmentListener{

    }
    ScreenSlidePageFragmentListener listener;
    public ScreenSlidePageFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if(context instanceof ScreenSlidePageFragmentListener){
            listener = (ScreenSlidePageFragmentListener) context;
        }
        this.context = context;
    }


    public static ScreenSlidePageFragment newInstance(String url) {
        ScreenSlidePageFragment fragment = new ScreenSlidePageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fakeR = new FakeR(getContext());
        if (getArguments() != null) {
            url = getArguments().getString(ARG_URL);

        }


    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                fakeR.getId("layout", "fragment_screen_slide_page"), container, false);
//        SimpleDraweeView zoomableDraweeView = (SimpleDraweeView) rootView.findViewById(fakeR.getId("id", "draweeView"));
        ZoomableDraweeView zoomableDraweeView = (ZoomableDraweeView) rootView.findViewById(fakeR.getId("id", "draweeView"));

        final ProgressBarDrawable progressBarDrawable = new ProgressBarDrawable();
        progressBarDrawable.setColor(this.context.getResources().getColor(fakeR.getId("color","colorAccent")));
        progressBarDrawable.setBackgroundColor(this.context.getResources().getColor(fakeR.getId("color","colorPrimaryDark")));
        progressBarDrawable
                .setRadius(5);
        final Drawable failureDrawable = this.context.getResources().getDrawable(fakeR.getId("drawable","missing"));
        DrawableCompat.setTint(failureDrawable, Color.RED);
        final Drawable placeholderDrawable = this.context.getResources().getDrawable(fakeR.getId("drawable","loading"));
        zoomableDraweeView.getHierarchy().setPlaceholderImage(placeholderDrawable, ScalingUtils.ScaleType.CENTER_INSIDE);
        zoomableDraweeView.getHierarchy().setFailureImage(failureDrawable, ScalingUtils.ScaleType.CENTER_INSIDE);
        zoomableDraweeView.getHierarchy().setProgressBarImage(progressBarDrawable, ScalingUtils.ScaleType.CENTER_INSIDE);

        zoomableDraweeView.setAllowTouchInterceptionWhileZoomed(true);
        // needed for double tap to zoom
        zoomableDraweeView.setIsLongpressEnabled(false);
        zoomableDraweeView.setTapListener(new DoubleTapGestureListener(zoomableDraweeView));
        int width = 1820;
        int height = 1820;
        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
                .setResizeOptions(new ResizeOptions(width, height))
                .build();
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setOldController(zoomableDraweeView.getController())
                .setImageRequest(request)
                .build();
        zoomableDraweeView.setController(controller);


        return rootView;
    }
}
