package com.creedon.cordova.plugin.captioninput;


import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
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
        SimpleDraweeView zoomableDraweeView = (SimpleDraweeView) rootView.findViewById(fakeR.getId("id", "draweeView"));
//        ZoomableDraweeView zoomableDraweeView = (ZoomableDraweeView) rootView.findViewById(fakeR.getId("id", "draweeView"));

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
        zoomableDraweeView.setEnabled(true);

        return rootView;
    }
}
