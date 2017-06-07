package com.creedon.cordova.plugin.captioninput;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.creedon.Nixplay.R;
import com.stfalcon.frescoimageviewer.drawee.ZoomableDraweeView;

/**
 * A simple {@link Fragment} subclass.
 */
public class ScreenSlidePageFragment extends Fragment {

    private static final String ARG_URL = "ARG_URL";
    private String url;

    public ScreenSlidePageFragment() {
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
        if (getArguments() != null) {
            url = getArguments().getString(ARG_URL);

        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_screen_slide_page, container, false);
        ZoomableDraweeView zoomableDraweeView = (ZoomableDraweeView) rootView.findViewById(R.id.draweeView);
        zoomableDraweeView.setImageURI(url);
        return rootView;
    }
}
