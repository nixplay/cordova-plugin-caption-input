package com.creedon.cordova.plugin.captioninput;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.creedon.androidphotobrowser.R;

import org.json.JSONArray;
import org.json.JSONObject;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (!Fresco.hasBeenInitialized()) {
            Fresco.initialize(this);
        }
        super.onCreate(savedInstanceState);

        fakeR = new FakeR(this.getApplicationContext());
        setContentView(fakeR.getId("layout", "activity_photoscaptioninput"));
        setupToolBar();

        String optionsJsonString = bundle.getString("options");
        try {
            JSONObject jsonObject = new JSONObject(optionsJsonString);
            JSONArray images = jsonObject.getJSONArray("images");
            JSONArray thumbnails = jsonObject.getJSONArray("thumbnails");
            JSONArray data = jsonObject.getJSONArray("data");
            JSONArray captions = jsonObject.getJSONArray("captions");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    protected void setupToolBar() {

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_up_white_24dp);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }
}
