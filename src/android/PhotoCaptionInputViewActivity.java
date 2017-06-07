package com.creedon.cordova.plugin.captioninput;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.creedon.Nixplay.R;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.stfalcon.frescoimageviewer.ImageViewer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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

public class PhotoCaptionInputViewActivity extends AppCompatActivity implements OverlayView.OverlayVieListener, RecyclerItemClickListener.OnItemClickListener {

    private FakeR fakeR;
    private ArrayList<String> captions;
    private int currentPosition;
//    private ImageViewer.OnImageChangeListener imageChangeListener = new ImageViewer.OnImageChangeListener() {
//        @Override
//        public void onImageChange(int position) {
//            currentPosition = position;
//            overlayView.setDescription(captions.get(position));
//        }
//    };
    private ImageViewer.OnDismissListener dismissListener = new ImageViewer.OnDismissListener() {
        @Override
        public void onDismiss() {
            finish();
        }
    };
    private ViewPager mPager;
    private ScreenSlidePagerAdapter mPagerAdapter;
//    private ImageViewer imageViewer;
//    protected OverlayView overlayView;

    @Override
    public void onDownloadButtonPressed(JSONObject data) {

    }

    @Override
    public void onTrashButtonPressed(JSONObject data) {

    }

    @Override
    public void onCaptionchnaged(JSONObject data, String caption) {
        captions.set(currentPosition, caption);
    }

    @Override
    public void onCloseButtonClicked() {
        finish();
    }

    @Override
    public List<String> getItemlist() {
        return poster;
    }
    private ArrayList<String> poster;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (!Fresco.hasBeenInitialized()) {
            Fresco.initialize(this);
        }
        super.onCreate(savedInstanceState);

        fakeR = new FakeR(this.getApplicationContext());
        setContentView(fakeR.getId("layout", "activity_photoscaptioninput"));
        setupToolBar();
        if (getIntent().getExtras() != null) {
            Bundle bundle = getIntent().getExtras();
            String optionsJsonString = bundle.getString("options");
            try {
                JSONObject jsonObject = new JSONObject(optionsJsonString);

//                String tc = jsonObject.getString("ts");
                JSONArray images = jsonObject.getJSONArray("images");
//                JSONArray preSelectedAssets = jsonObject.getJSONArray("preSelectedAssets");
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

                poster = stringArray;
//                showPicker(poster);

                mPager = (ViewPager) findViewById(R.id.pager);
                mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(),poster);
                mPager.setAdapter(mPagerAdapter);



                RecyclerView recyclerView = (RecyclerView) findViewById(fakeR.getId("id", "recycleview"));
                LinearLayoutManager linearLayoutManager2 = new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false);
                RecyclerViewAdapter recyclerViewAdapter2 = new RecyclerViewAdapter(this, stringArray);
                recyclerView.setHasFixedSize(true);
                recyclerView.setLayoutManager(linearLayoutManager2);
                recyclerView.setAdapter(recyclerViewAdapter2);
                recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this, recyclerView, this));

                //show image view

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
    protected void setupToolBar() {

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_up_white_24dp);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }
    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(fakeR.getId("menu", "menu_photoscaptioninput"), menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        int id = item.getItemId();
        if (id == android.R.id.home) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return true;
        }else if(id == fakeR.getId("id","btnTrash")){

        }
        return onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

//    private void showPicker(ArrayList<String> stringArray) {
//        overlayView = new OverlayView(this);
//        imageViewer = new ImageViewer.Builder(this, stringArray)
//                .setCustomImageRequestBuilder(ImageViewer
//                        .createImageRequestBuilder()
//                        .setResizeOptions(
//                                ResizeOptions
//                                        //TODO can be more dynamic
//                                        .forDimensions(1820, 1820)
//                        )
//                )
//                .setOverlayView(overlayView)
//                .setStartPosition(currentPosition)
//                .setImageChangeListener(getImageChangeListener())
//                .setOnDismissListener(getDismissListener())
//                .show();
//
//    }



    @Override
    public void onItemClick(View view, int position) {

    }

    @Override
    public void onItemLongClick(View view, int position) {

    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        private final ArrayList<String> itemList;

        public ScreenSlidePagerAdapter(FragmentManager fm, ArrayList<String>itemList) {
            super(fm);
            this.itemList = itemList;
        }

        @Override
        public Fragment getItem(int position) {
            return ScreenSlidePageFragment.newInstance(itemList.get(position));
        }

        @Override
        public int getCount() {
            return getItemlist().size();
        }
    }

//    public ImageViewer.OnImageChangeListener getImageChangeListener() {
//        return imageChangeListener;
//    }
//
//    public ImageViewer.OnDismissListener getDismissListener() {
//        return dismissListener;
//    }
//
//    public ImageViewer.Formatter getImageFormatter() {
//        return new ImageViewer.Formatter<CustomImage>() {
//            @Override
//            public String format(CustomImage customImage) {
//                return customImage.getUrl();
//            }
//        };
//    }
}
