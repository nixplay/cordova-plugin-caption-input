package com.creedon.cordova.plugin.captioninput;


import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

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

    private ZoomableDraweeView zoomableDraweeView;
    private ImageView playButton;
    private VideoView videoView;
    private Handler mHandler = new Handler();

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
        zoomableDraweeView = (ZoomableDraweeView) rootView.findViewById(fakeR.getId("id", "draweeView"));
        playButton = (ImageView) rootView.findViewById(fakeR.getId("id", "iv_play"));
        videoView = (VideoView) rootView.findViewById(fakeR.getId("id", "videoView"));

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

        if (isVideo(url)) {
            playButton.setVisibility(View.VISIBLE);

            videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.d("ScreenSlidePageFragment", "Video ended");
                    zoomableDraweeView.setVisibility(View.VISIBLE);
                    playButton.setVisibility(View.VISIBLE);
                    videoView.setVisibility(View.INVISIBLE);
                }
            });

            playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {


                    videoView.setVideoURI(Uri.parse(url));
                    MediaController mediaController = new MediaController(getContext());
                    mediaController.setVisibility(View.GONE);
                    videoView.setMediaController(mediaController);
                    videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mp.setLooping(false);
                            mp.start();

                            float multiplier = (float) videoView.getHeight() / (float) mp.getVideoHeight();
                            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams( ((int) (mp.getVideoWidth() * multiplier)), ViewGroup.LayoutParams.MATCH_PARENT);
                            params.gravity = Gravity.CENTER;
                            videoView.setLayoutParams(params);
                        }
                    });
                    videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            playButton.setVisibility(View.VISIBLE);
                            videoView.setVisibility(View.INVISIBLE);
                            zoomableDraweeView.setVisibility(View.VISIBLE);

                        }
                    });
                    playButton.setVisibility(View.INVISIBLE);
                    videoView.setVisibility(View.VISIBLE);
                    //delay added for smooth transition from video_preview (drawee) -> video (videoView) when video is played initially
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            zoomableDraweeView.setVisibility(View.INVISIBLE);
                        }
                    }, 100);
                }
            });


        } else {
            playButton.setVisibility(View.INVISIBLE);
            videoView.setVisibility(View.INVISIBLE);
        }


        return rootView;
    }

    private static boolean isVideo(String filePath) {
        filePath = filePath.toLowerCase();
        return filePath.endsWith(".mp4");
    }

    public void stopVideoPlayback() {
        if (videoView != null && videoView.isPlaying()) {
            videoView.stopPlayback();
            videoView.setVisibility(View.INVISIBLE);
            zoomableDraweeView.setVisibility(View.VISIBLE);
            playButton.setVisibility(View.VISIBLE);
        }
    }
}
