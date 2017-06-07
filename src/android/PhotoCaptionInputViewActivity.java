package com.creedon.cordova.plugin.captioninput;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.creedon.androidphotobrowser.common.data.models.CustomImage;
import com.creedon.androidphotobrowser.common.views.ImageOverlayView;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.stfalcon.frescoimageviewer.ImageViewer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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

public class PhotoCaptionInputViewActivity extends AppCompatActivity {

    private FakeR fakeR;
    private ArrayList<String> captions;
    private int currentPosition;
    private ImageViewer.OnImageChangeListener imageChangeListener = new ImageViewer.OnImageChangeListener() {
        @Override
        public void onImageChange(int position) {
            currentPosition = position;
            overlayView.setDescription(captions.get(position));
        }
    };
    private ImageViewer.OnDismissListener dismissListener = new ImageViewer.OnDismissListener() {
        @Override
        public void onDismiss() {
            finish();
        }
    };
    private ImageViewer imageViewer;
    protected OverlayView overlayView;
    private ImageOverlayView.ImageOverlayVieListener imageOverlayViewListener = new ImageOverlayView.ImageOverlayVieListener() {
        @Override
        public void onDownloadButtonPressed(JSONObject data) {

        }

        @Override
        public void onTrashButtonPressed(JSONObject data) {

        }

        @Override
        public void onCaptionchnaged(JSONObject data, String caption) {

        }

        @Override
        public void onCloseButtonClicked() {
            finish();
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (!Fresco.hasBeenInitialized()) {
            Fresco.initialize(this);
        }
        super.onCreate(savedInstanceState);

        fakeR = new FakeR(this.getApplicationContext());
        setContentView(fakeR.getId("layout", "activity_photoscaptioninput"));
//        setupToolBar();
        if (getIntent().getExtras() != null) {
            Bundle bundle = getIntent().getExtras();
            String optionsJsonString = bundle.getString("options");
            try {
                JSONObject jsonObject = new JSONObject(optionsJsonString);

                String tc = jsonObject.getString("ts");
                JSONArray images = jsonObject.getJSONArray("images");
                JSONArray preSelectedAssets = jsonObject.getJSONArray("preSelectedAssets");
                if (!jsonObject.get("friends").equals(null)) {
                    JSONArray friends = jsonObject.getJSONArray("friends");
                }

                ArrayList<String> stringArray = new ArrayList<String>();
                captions = new ArrayList<String>();

                for (int i = 0, count = images.length(); i < count; i++) {
                    try {
                        String jsonObj = images.getString(i);
                        stringArray.add(jsonObj);
                        captions.add(i,"");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                currentPosition = 0;
                overlayView = new OverlayView(this);
                overlayView.setListener(this.imageOverlayViewListener);


                showPicker(stringArray);

                //show image view
                //add toobar item
            } catch (Exception e) {
                e.printStackTrace();
                //TODO setresult failed
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        } else {
            //TODO setresult failed
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    private void showPicker(ArrayList<String> stringArray) {
        imageViewer = new ImageViewer.Builder(this, stringArray)
                .setCustomImageRequestBuilder(ImageViewer
                        .createImageRequestBuilder()
                        .setResizeOptions(
                                ResizeOptions
                                        .forDimensions(1820, 1820)
                        )
                )
                .setOverlayView(overlayView)
                .setStartPosition(currentPosition)
                .setImageChangeListener(getImageChangeListener())
                .setOnDismissListener(getDismissListener())
                .show();

    }

//    protected void setupToolBar() {
//
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_up_white_24dp);
//
//    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }
//    @Override
//    public boolean onCreatePanelMenu(int featureId, Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//
//        inflater.inflate(fakeR.getId("menu", "menu_photoscaptioninput"), menu);
//
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle item selection
//        int id = item.getItemId();
//        if (id == android.R.id.home) {
//            setResult(Activity.RESULT_CANCELED);
//            finish();
//            return true;
//        }
////        else if(id == fakeR.getId("id","btnTrash")){
////
////        }
//        return onOptionsItemSelected(item);
//    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(Activity.RESULT_CANCELED);
        imageViewer.onDismiss();
        finish();
    }

    public ImageViewer.OnImageChangeListener getImageChangeListener() {
        return imageChangeListener;
    }

    public ImageViewer.OnDismissListener getDismissListener() {
        return dismissListener;
    }

    public ImageViewer.Formatter getImageFormatter() {
        return new ImageViewer.Formatter<CustomImage>() {
            @Override
            public String format(CustomImage customImage) {
                return customImage.getUrl();
            }
        };
    }
}
