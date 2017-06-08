package com.creedon.cordova.plugin.captioninput;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.rengwuxian.materialedittext.MaterialEditText;

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

public class PhotoCaptionInputViewActivity extends AppCompatActivity implements RecyclerItemClickListener.OnItemClickListener {

    private static final String KEY_IMAGE = "image";
    private static final String KEY_CAPTION = "caption";
    private FakeR fakeR;
    private ArrayList<String> captions;
    private int currentPosition;


    private ViewPager mPager;
    private ScreenSlidePagerAdapter mPagerAdapter;
    private MaterialEditText mEditText;
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private RecyclerViewAdapter recyclerViewAdapter;


    public List<String> getItemlist() {
        return imageList;
    }

    private ArrayList<String> imageList;


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

                String tc = jsonObject.getString("ts");
                JSONArray imagesJsonArray = jsonObject.getJSONArray("images");
                JSONArray preSelectedAssets = jsonObject.getJSONArray("preSelectedAssets");
                try {
                    if (jsonObject.get("friends") != null) {
                        JSONArray friends = jsonObject.getJSONArray("friends");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                ArrayList<String> stringArray = new ArrayList<String>();
                captions = new ArrayList<String>();

                for (int i = 0, count = imagesJsonArray.length(); i < count; i++) {
                    try {
                        String jsonObj = imagesJsonArray.getString(i);
                        stringArray.add(jsonObj);
                        captions.add(i, "");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                currentPosition = 0;

                imageList = stringArray;

                mEditText = (MaterialEditText) findViewById(fakeR.getId("id", "etDescription"));
                mEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        captions.set(currentPosition, editable.toString());
                    }
                });
                Button submitButton = (Button) findViewById(fakeR.getId("id", "btnSubmit"));
                submitButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        try {
                            finishWithResult();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            finishActivity(-1);
                        }
                    }
                });
                mPager = (ViewPager) findViewById(fakeR.getId("id", "pager"));
                mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    public void onPageScrollStateChanged(int state) {
                    }

                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    }

                    public void onPageSelected(int position) {
                        // Check if this is the page you want.
                        if (currentPosition != mPager.getCurrentItem()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    currentPosition = mPager.getCurrentItem();
                                    setActionBarTitle(imageList, currentPosition);
                                    mEditText.setText(captions.get(currentPosition));
                                    linearLayoutManager.scrollToPositionWithOffset(currentPosition, -1);
                                }
                            });
                        }
                    }
                });
                mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(), stringArray);
                mPager.setAdapter(mPagerAdapter);


                recyclerView = (RecyclerView) findViewById(fakeR.getId("id", "recycleview"));
                linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
                recyclerViewAdapter = new RecyclerViewAdapter(this, stringArray);
                recyclerView.setHasFixedSize(true);
                recyclerView.setLayoutManager(linearLayoutManager);
                recyclerView.setAdapter(recyclerViewAdapter);
                recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this, recyclerView, this));
                setActionBarTitle(imageList, currentPosition);
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
        getSupportActionBar().setHomeAsUpIndicator(fakeR.getId("drawable", "ic_up_white_24dp"));

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
        } else if (id == fakeR.getId("id", "btnTrash")) {
            //delete current page image
            if (imageList.size() > 0) {
                imageList.remove(currentPosition);
                captions.remove(currentPosition);
                currentPosition = Math.max(0, Math.min(currentPosition, imageList.size() - 1));
                if (imageList.size() == 0) {

                    finishActivity(RESULT_CANCELED);
                    finish();
                    return false;
                } else {
                    ArrayList<String> clonedPoster = (ArrayList<String>) imageList.clone();
                    ArrayList<String> clonedPoster2 = (ArrayList<String>) imageList.clone();
                    mPagerAdapter.swap(clonedPoster);
                    recyclerViewAdapter.swap(clonedPoster2);
                    linearLayoutManager.scrollToPositionWithOffset(currentPosition, -1);
                    setActionBarTitle(imageList, currentPosition);
                }
            } else {
                finishActivity(RESULT_CANCELED);
                return false;
            }

            return true;
        }
        return onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onItemClick(View view, int position) {
        currentPosition = position;
        mPager.setCurrentItem(position);
        setActionBarTitle(imageList, currentPosition);
        mEditText.setText(captions.get(currentPosition));

    }

    @Override
    public void onItemLongClick(View view, int position) {
        currentPosition = position;
        mPager.setCurrentItem(position);
        setActionBarTitle(imageList, currentPosition);
        mEditText.setText(captions.get(currentPosition));
    }



    public void setActionBarTitle(ArrayList<String> actionBarTitle, int index) {

        getSupportActionBar().setTitle((index + 1) + "/" + actionBarTitle.size());
    }

    private void finishWithResult() throws JSONException {
        Bundle conData = new Bundle();
        JSONArray jsonArray = new JSONArray();
        for (int i = 0, count = imageList.size(); i < count; i++) {
            JSONObject object = new JSONObject();
            object.put(KEY_IMAGE, imageList.get(i));
            object.put(KEY_CAPTION, captions.get(i));
            jsonArray.put(object);
        }

        conData.putString("result", jsonArray.toString());
        Intent intent = new Intent();
        intent.putExtras(conData);
        setResult(RESULT_OK, intent);
        finishActivity(Constants.REQUEST_SUBMIT);
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        private ArrayList<String> itemList;

        public ScreenSlidePagerAdapter(FragmentManager fm, ArrayList<String> itemList) {
            super(fm);
            this.itemList = itemList;
        }

        @Override
        public Fragment getItem(int position) {
            return ScreenSlidePageFragment.newInstance(itemList.get(position));
        }

        @Override
        public int getCount() {
            return itemList.size();
        }

        public void swap(ArrayList<String> poster) {
            itemList = poster;

            runOnUiThread(new Runnable() {
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }

        //        https://stackoverflow.com/questions/13695649/refresh-images-on-fragmentstatepageradapter-on-resuming-activity
        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }


}
