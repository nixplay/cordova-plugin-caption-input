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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.kbeanie.multipicker.api.ImagePicker;
import com.kbeanie.multipicker.api.Picker;
import com.kbeanie.multipicker.api.callbacks.ImagePickerCallback;
import com.kbeanie.multipicker.api.entity.ChosenImage;

import com.rengwuxian.materialedittext.MaterialEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.creedon.cordova.plugin.captioninput.Constants.KEY_CAPTION;
import static com.creedon.cordova.plugin.captioninput.Constants.KEY_IMAGE;
import static com.creedon.cordova.plugin.captioninput.Constants.KEY_INVALIDIMAGES;
import static com.creedon.cordova.plugin.captioninput.Constants.KEY_PRESELECT;

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


    private static final int REQUEST_CODE_PHOTO_INPUT = 0x31;
    private static final String TAG = PhotoCaptionInputViewActivity.class.getSimpleName();


    private FakeR fakeR;
    private ArrayList<String> captions;
    private int currentPosition;


    private ViewPager mPager;
    private ScreenSlidePagerAdapter mPagerAdapter;
    private MaterialEditText mEditText;
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private RecyclerViewAdapter recyclerViewAdapter;
    private int width;
    private int height;
    private int quality;
    private ImagePicker imagePicker;
    private KProgressHUD kProgressHUD;


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
//                this.width = jsonObject.getInt("width");
//                this.height = jsonObject.getInt("height");
//                this.quality = jsonObject.getInt("quality");
                JSONArray imagesJsonArray = jsonObject.getJSONArray("images");
                JSONArray preSelectedAssets = jsonObject.getJSONArray("preSelectedAssets");
//                try {
//                    if (jsonObject.get("friends") != null) {
//                        JSONArray friends = jsonObject.getJSONArray("friends");
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }

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
                ( (ImageButton) findViewById(fakeR.getId("id", "btnSubmit"))).setOnClickListener(new View.OnClickListener() {
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

                ((ImageButton) findViewById(fakeR.getId("id", "btnAdd"))).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

//                        TODO James Kong 2017-06-09 IDEAL image picker with replace with existing
                        imagePicker = new ImagePicker(PhotoCaptionInputViewActivity.this);
                        imagePicker.setImagePickerCallback(new ImagePickerCallback() {
                            @Override
                            public void onImagesChosen(List<ChosenImage> images) {
                                //dismiss dialog
                                if(kProgressHUD != null){
                                    kProgressHUD.dismiss();
                                }
                                // Display images
                                for (ChosenImage image : images) {
                                    File file = new File(image.getOriginalPath());

                                    imageList.add("file://"+file.getAbsolutePath());
                                    captions.add("");
                                    Log.d(TAG, image.toString());
                                }

                                setActionBarTitle(imageList, currentPosition);
                                refreshList();

                            }

                            @Override
                            public void onError(String message) {
                                // Do error handling
                                Log.d(TAG, message);
                            }
                        });

                        imagePicker.allowMultiple(); // Default is false
                        imagePicker.shouldGenerateMetadata(false); // Default is true
                        imagePicker.shouldGenerateThumbnails(false); // Default is true
                        imagePicker.pickImage();

                    }
                });
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK) {
            if(requestCode == Picker.PICK_IMAGE_DEVICE) {
                kProgressHUD = KProgressHUD.create(PhotoCaptionInputViewActivity.this)
                        .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                        .setDetailsLabel("Downloading data")
                        .setCancellable(false)
                        .setAnimationSpeed(2)
                        .setDimAmount(0.5f)
                        .show();

                imagePicker.submit(data);
                imagePicker.submit(data);
            }
        }

//        if (resultCode == Activity.RESULT_OK && data != null && requestCode == REQUEST_CODE_PHOTO_INPUT) {
//            ArrayList<String> fileNames = data.getStringArrayListExtra("MULTIPLEFILENAMES");
//            ArrayList<String> preSelectedAssets = data.getStringArrayListExtra("SELECTED_ASSETS");
//            ArrayList<String> invalidImages = data.getStringArrayListExtra("INVALID_IMAGES");
//            for(int i = fileNames.size() - imageList.size() , count = fileNames.size(); i< count; i++){
//                captions.add("");
//            }
//            imageList.clear();
//            imageList.addAll(fileNames);
//            refreshList();
//        }
    }
    public void setActionBarTitle(ArrayList<String> actionBarTitle, int index) {

        getSupportActionBar().setTitle((index + 1) + "/" + actionBarTitle.size());
    }

    private void finishWithResult() throws JSONException {
        Bundle conData = new Bundle();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(KEY_IMAGE , imageList);
        jsonObject.put(KEY_CAPTION , captions);
        jsonObject.put(KEY_PRESELECT,new JSONArray());
        jsonObject.put(KEY_INVALIDIMAGES,new JSONArray());
        conData.putString(Constants.RESULT, jsonObject.toString());
        Intent intent = new Intent();
        intent.putExtras(conData);
        setResult(RESULT_OK, intent);
        finishActivity(Constants.REQUEST_SUBMIT);
    }
    void refreshList(){
        ArrayList<String> clonedPoster = (ArrayList<String>) imageList.clone();
        mPagerAdapter.swap(clonedPoster);
        ArrayList<String> clonedPoster2 = (ArrayList<String>) imageList.clone();
        recyclerViewAdapter.swap(clonedPoster);
        mPagerAdapter.swap(clonedPoster2);
        setActionBarTitle(imageList, currentPosition);

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
