
package com.creedon.cordova.plugin.captioninput;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.internal.entity.CaptureStrategy;
import com.zhihu.matisse.internal.utils.PathUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static com.creedon.cordova.plugin.captioninput.Constants.KEY_CAPTIONS;
import static com.creedon.cordova.plugin.captioninput.Constants.KEY_IMAGES;
import static com.creedon.cordova.plugin.captioninput.Constants.KEY_INVALIDIMAGES;
import static com.creedon.cordova.plugin.captioninput.Constants.KEY_PRESELECTS;

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

public class PhotoCaptionInputViewActivity extends AppCompatActivity implements RecyclerItemClickListener.OnItemClickListener, RecyclerViewAdapter.RecyclerViewAdapterListener {

    private static final String TAG = PhotoCaptionInputViewActivity.class.getSimpleName();
    private static final String KEY_LABEL = "label";
    private static final String KEY_TYPE = "type";
    private static final int MAX_CHARACTOR = 160;
    private static final int REQUEST_CODE_PICKER = 0x111;
    private static final int REQUEST_CODE_CHOOSE = 0x111;
    private FakeR fakeR;
    private ArrayList<String> captions;
    private int currentPosition;
    private ViewPager mPager;
    private ScreenSlidePagerAdapter mPagerAdapter;
    private MaterialEditText mEditText;
    private LinearLayoutManager linearLayoutManager;
    private RecyclerViewAdapter recyclerViewAdapter;

    private KProgressHUD kProgressHUD;
    private ArrayList<String> imageList;
    private int width, height;
    private JSONArray buttonOptions;
    ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
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
                        if (recyclerViewAdapter != null) {
                            recyclerViewAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        }
    };
    private ViewPager.OnAdapterChangeListener onAdapterChangeListener = new ViewPager.OnAdapterChangeListener() {
        @Override
        public void onAdapterChanged(@NonNull ViewPager viewPager, @Nullable PagerAdapter oldAdapter, @Nullable PagerAdapter newAdapter) {

        }
    };
    private ArrayList<String> preSelectedAssets = new ArrayList<String>();
    private int maxImages;


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (!Fresco.hasBeenInitialized()) {
            Fresco.initialize(this);

        }
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);

        fakeR = new FakeR(this.getApplicationContext());
        setContentView(fakeR.getId("layout", "activity_photoscaptioninput"));
        setupToolBar();

        SharedPreferences sharedPrefs = getSharedPreferences("group.com.creedon.Nixplay", Context.MODE_PRIVATE);
        boolean isOptimizeSize = false;
        try{
            isOptimizeSize = sharedPrefs.getBoolean("nixSettings.settings.resolution",false);
        }catch(Exception e){
            try {
                String ret = sharedPrefs.getString("nixSettings.settings.resolution", "");
                isOptimizeSize = Boolean.parseBoolean(ret);
            }catch (Exception e1){
                e1.printStackTrace();
            }
        }
        if(isOptimizeSize) {
            this.width = 1820;
            this.height = 1820;
        }
        if (getIntent().getExtras() != null) {
            Bundle bundle = getIntent().getExtras();
            String optionsJsonString = bundle.getString("options");
            this.maxImages = bundle.getInt("MAX_IMAGES");
            try {
                JSONObject jsonObject = new JSONObject(optionsJsonString);

                try {
                    this.buttonOptions = jsonObject.getJSONArray("buttons");
                } catch (Exception e) {
                    e.printStackTrace();

                }
                JSONArray imagesJsonArray = jsonObject.getJSONArray("images");
                JSONArray preSelectedAssetsJsonArray = jsonObject.getJSONArray("preSelectedAssets");

                if (preSelectedAssetsJsonArray != null) {
                    for (int i = 0; i < preSelectedAssetsJsonArray.length(); i++) {
                        preSelectedAssets.add(preSelectedAssetsJsonArray.getString(i));
                    }
                }

                ArrayList<String> stringArray = new ArrayList<String>();
                captions = new ArrayList<String>();

                for (int i = 0, count = imagesJsonArray.length(); i < count; i++) {
                    try {
                        String jsonObj = imagesJsonArray.getString(i);

                        if (jsonObj.contains("file:///")) {
                            stringArray.add(jsonObj);
                        } else {
                            stringArray.add(Uri.fromFile(new File(jsonObj)).toString());
                        }

                        captions.add(i, "");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                currentPosition = 0;

                imageList = stringArray;

                mEditText = (MaterialEditText) findViewById(fakeR.getId("id", "etDescription"));
                mEditText.setHint(fakeR.getId("string", "ADD_CAPTION"));
                mEditText.setFloatingLabelText(getString(fakeR.getId("string", "ADD_CAPTION")));
                InputFilter[] filterArray = new InputFilter[1];
                filterArray[0] = new InputFilter.LengthFilter(MAX_CHARACTOR);
                mEditText.setFilters(filterArray);
                mEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        mEditText.setFloatingLabelText(getString(fakeR.getId("string", "ADD_CAPTION")) + "(" + charSequence.length() + "/" + MAX_CHARACTOR + ")");
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        if (editable.length() > 0) {
                            if (editable.charAt(editable.length() - 1) == '\n') {
                                editable.delete(editable.length() - 1, editable.length());
                                mEditText.clearFocus();
                                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);

                            }

                            captions.set(currentPosition, editable.toString());
                        }
                    }
                });
                mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        Log.d("onEditorAction", " TextView " + v.toString() + " actionId " + actionId + " event " + event);
                        return false;
                    }
                });
                if (this.buttonOptions != null) {
                    for (int i = 0; i < this.buttonOptions.length(); i++) {
                        JSONObject obj = (JSONObject) this.buttonOptions.get(i);
                        String label = obj.getString(KEY_LABEL);
                        final String type = obj.getString(KEY_TYPE);
                        if (i == 0) {
                            if (this.buttonOptions.length() == 1) {
                                findViewById(fakeR.getId("id", "button1")).setVisibility(GONE);
                                Button button = (Button) findViewById(fakeR.getId("id", "button2"));
                                button.setText(label);
                                button.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        try {
                                            finishWithResult(type);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                            finishActivity(-1);
                                        }
                                    }
                                });
                            } else {
                                Button button1 = (Button) findViewById(fakeR.getId("id", "button1"));
                                button1.setText(label);
                                button1.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        try {
                                            finishWithResult(type);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                            finishActivity(-1);
                                        }
                                    }
                                });
                            }
                        } else if (i == 1) {


                            Button button2 = (Button) findViewById(fakeR.getId("id", "button2"));
                            button2.setText(label);
                            button2.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    try {
                                        finishWithResult(type);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        finishActivity(-1);
                                    }
                                }
                            });

                        }
                    }

                }
                mPager = (ViewPager) findViewById(fakeR.getId("id", "pager"));
                mPager.addOnPageChangeListener(onPageChangeListener);
                mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(), stringArray);
                mPager.setAdapter(mPagerAdapter);

                RecyclerView recyclerView = (RecyclerView) findViewById(fakeR.getId("id", "recycleview"));
                linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
                recyclerViewAdapter = new RecyclerViewAdapter(this, stringArray);
                recyclerView.setHasFixedSize(true);
                recyclerView.setLayoutManager(linearLayoutManager);
                recyclerView.setAdapter(recyclerViewAdapter);
                recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this, recyclerView, this));
                setActionBarTitle(imageList, currentPosition);

                (findViewById(fakeR.getId("id", "btnAdd"))).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ArrayList<Uri> serialPreselectedAssets = new ArrayList<Uri>();
                        for(String s : preSelectedAssets){
                            serialPreselectedAssets.add(Uri.parse(s));
                        }
                        Matisse.from(PhotoCaptionInputViewActivity.this)
                                .choose(MimeType.of(
                                        MimeType.JPEG,
                                        MimeType.PNG

                                ), true)
                                .countable(true)
                                .capture(true)
                                .captureStrategy(
                                        new CaptureStrategy(true, getApplication().getPackageName()+".provider"))
                                .maxSelectable(PhotoCaptionInputViewActivity.this.maxImages)
                                .gridExpectedSize((int) convertDpToPixel(120,PhotoCaptionInputViewActivity.this))
                                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                                .thumbnailScale(0.85f)
                                .imageEngine(new GlideEngine())
                                .enablePreview(false)
                                .showUseOrigin(false)
                                .showSingleMediaType(true)
                                .forResult(REQUEST_CODE_CHOOSE, serialPreselectedAssets);

                    }
                });
                //show image view

            } catch (Exception e) {
                e.printStackTrace();
                //TODO setresult failed
                Intent intent = new Intent();
                setResult(RESULT_CANCELED, intent);
                finish();
            }
        } else {
            //TODO setresult failed
            Intent intent = new Intent();
            setResult(RESULT_CANCELED, intent);
            finish();
        }
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    protected void setupToolBar() {
        try {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {

                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setHomeAsUpIndicator(fakeR.getId("drawable", "ic_close_gray_24dp"));

            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

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
                preSelectedAssets.remove(currentPosition);
                captions.remove(currentPosition);
                recyclerViewAdapter.notifyItemRemoved(currentPosition);
                currentPosition = Math.max(0, Math.min(currentPosition, imageList.size() - 1));
                recyclerViewAdapter.notifyItemChanged(currentPosition);
                if (imageList.size() == 0) {

                    finishActivity(RESULT_CANCELED);
                    finish();
                    return false;
                } else {
                    ArrayList<String> clonedPoster = new ArrayList<String>(imageList);
                    ArrayList<String> clonedPoster2 = new ArrayList<String>(imageList);
                    mPagerAdapter.swap(clonedPoster);
                    recyclerViewAdapter.swap(clonedPoster2);
                    linearLayoutManager.scrollToPositionWithOffset(currentPosition, -1);
                    setActionBarTitle(imageList, currentPosition);
                    mEditText.setText(captions.get(currentPosition));
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
        try {
            currentPosition = (position > 0 && position < imageList.size()) ? position : 0;
            mPager.setCurrentItem(currentPosition);
            setActionBarTitle(imageList, currentPosition);
            mEditText.setText(captions.get(currentPosition));
            if (recyclerViewAdapter.getItemCount() > 0 || recyclerViewAdapter != null) {
                recyclerViewAdapter.notifyDataSetChanged();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void onItemLongClick(View view, int position) {
        try {
            currentPosition = (position > 0 && position < imageList.size()) ? position : 0;
            mPager.setCurrentItem(currentPosition);
            setActionBarTitle(imageList, currentPosition);
            mEditText.setText(captions.get(currentPosition));
            Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 500 milliseconds
            v.vibrate(500);
            if (recyclerViewAdapter.getItemCount() > 0 || recyclerViewAdapter != null) {
                recyclerViewAdapter.notifyDataSetChanged();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_CHOOSE){
                ArrayList<String> newImages = new ArrayList<String>();
                ArrayList<String> newPreselectedAssets = new ArrayList<String>();
                ArrayList<String> newCaptions = new ArrayList<String>();

                List<Uri> result = Matisse.obtainResult(data);
                for (int i = 0 ; i < result.size() ; i++) {
                    String fileActualPath = PathUtils.getPath(getApplicationContext(),result.get(i));
                    String uriString = result.get(i).toString();
                    int index = preSelectedAssets.indexOf(uriString);
                    if (fileActualPath.contains("file:///")) {
                        newImages.add(fileActualPath);
                    } else {
                        newImages.add(Uri.fromFile(new File(fileActualPath)).toString());
                    }

                    newPreselectedAssets.add(uriString);
                    if(preSelectedAssets.contains(uriString)){
                        if(index>0 && index < captions.size()){
                            newCaptions.add(captions.get(index));
                        }else{
                            newCaptions.add("");
                        }
                    }else{
                        newCaptions.add("");
                    }

                }
                imageList = newImages;
                preSelectedAssets = newPreselectedAssets;
                captions = newCaptions;


                refreshList();
            }
        }

    }

    public void setActionBarTitle(ArrayList<String> actionBarTitle, int index) {
        try {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle((index + 1) + "/" + actionBarTitle.size());
            }
            mEditText.setText(captions.get(index));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void finishWithResult(final String type) throws JSONException {
//        Bundle conData = new Bundle();
//        try {
//            JSONObject jsonObject = new JSONObject();
//
//            JSONArray array = new JSONArray(imageList);
//
//            jsonObject.put(KEY_IMAGES, array);
//            jsonObject.put(KEY_CAPTIONS, new JSONArray(captions));
//            jsonObject.put(KEY_PRESELECTS, new JSONArray());
//            jsonObject.put(KEY_INVALIDIMAGES, new JSONArray());
//            conData.putString(Constants.RESULT, jsonObject.toString());
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        Intent intent = new Intent();
//        intent.putExtras(conData);
//        setResult(RESULT_OK, intent);
//        finishActivity(Constants.REQUEST_SUBMIT);
//        finish();

        //for testing james 20170615
        recyclerViewAdapter = null;
        if (mPager != null) {
            mPager.removeOnPageChangeListener(onPageChangeListener);
            mPager.removeOnAdapterChangeListener(onAdapterChangeListener);
        }
        mPager = null;
        mPagerAdapter = null;
        if (kProgressHUD != null) {
            if (kProgressHUD.isShowing()) {
                kProgressHUD.dismiss();
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                kProgressHUD = KProgressHUD.create(PhotoCaptionInputViewActivity.this)
                        .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                        .setDetailsLabel(getString(fakeR.getId("string", "LOADING")))
                        .setCancellable(false)
                        .setAnimationSpeed(2)
                        .setDimAmount(0.5f)
                        .show();

            }
        });
        ImageResizer imageResizer = new ImageResizer(this, imageList);

        ImageResizeTask task = new ImageResizeTask();

        imageResizer.setCallback(new ResizeCallback() {

            @Override
            public void onResizeSuccess(ArrayList<String> outList) {
                kProgressHUD.dismiss();
                Bundle conData = new Bundle();
                try {
                    JSONObject jsonObject = new JSONObject();

                    JSONArray array = new JSONArray(outList);

                    jsonObject.put(KEY_IMAGES, array);
                    jsonObject.put(KEY_CAPTIONS, new JSONArray(captions));
                    jsonObject.put(KEY_PRESELECTS, new JSONArray());
                    jsonObject.put(KEY_INVALIDIMAGES, new JSONArray());
                    jsonObject.put(KEY_TYPE, type);
                    conData.putString(Constants.RESULT, jsonObject.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent();
                intent.putExtras(conData);
                setResult(RESULT_OK, intent);
                finishActivity(Constants.REQUEST_SUBMIT);
                finish();
            }

            @Override
            public void onResizeFailed(String s) {
                kProgressHUD.dismiss();
                Log.e(TAG, s);
                Intent intent = new Intent();

                setResult(RESULT_CANCELED, intent);
                finish();

            }
        });
        task.execute(imageResizer);

    }


    protected String storeImage(String inFilePath, String inFileName) throws JSONException, IOException, URISyntaxException {


        String outFilePath = System.getProperty("java.io.tmpdir") + "/";
        if(!(inFilePath + File.separator + inFileName).equals(outFilePath + inFileName)) {
            copyFile(inFilePath + File.separator, inFileName, outFilePath);
            try {
                copyExif(inFilePath + File.separator + inFileName, outFilePath + inFileName);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return outFilePath + inFileName;
        }
        return inFilePath + File.separator + inFileName;

    }

    protected String storeImageWithExif(String inFileName, Bitmap bmp, ExifInterface exif) throws JSONException, IOException, URISyntaxException {

        String filename = inFileName;
        filename.replace("bmp", "jpg");
        String filePath = System.getProperty("java.io.tmpdir") + "/" + filename;
//        exif.writeExif(bmp, filePath, 100);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(filePath);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        copyExif(exif, filePath);
        return filePath;

    }

    private void copyFile(String inputPath, String inputFile, String outputPath) {

        FileInputStream in = null;
        OutputStream out = null;
        try {

            //create output directory if it doesn't exist
            File dir = new File(outputPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }


            in = new FileInputStream(inputPath + inputFile);
            out = new FileOutputStream(outputPath + inputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file (You have now copied the file)
            out.flush();
            out.close();
            out = null;

        } catch (FileNotFoundException fnfe1) {
            Log.e("tag", fnfe1.getMessage());
        } catch (Exception e) {
            Log.e("tag", e.getMessage());
        }

    }

    public static void copyExif(String oldPath, String newPath) throws IOException {
        ExifInterface oldExif = new ExifInterface(oldPath);

        String[] attributes = new String[]
                {
                        ExifInterface.TAG_APERTURE,
                        ExifInterface.TAG_DATETIME,
                        ExifInterface.TAG_DATETIME_DIGITIZED,
                        ExifInterface.TAG_EXPOSURE_TIME,
                        ExifInterface.TAG_FLASH,
                        ExifInterface.TAG_FOCAL_LENGTH,
                        ExifInterface.TAG_GPS_ALTITUDE,
                        ExifInterface.TAG_GPS_ALTITUDE_REF,
                        ExifInterface.TAG_GPS_DATESTAMP,
                        ExifInterface.TAG_GPS_LATITUDE,
                        ExifInterface.TAG_GPS_LATITUDE_REF,
                        ExifInterface.TAG_GPS_LONGITUDE,
                        ExifInterface.TAG_GPS_LONGITUDE_REF,
                        ExifInterface.TAG_GPS_PROCESSING_METHOD,
                        ExifInterface.TAG_GPS_TIMESTAMP,
                        ExifInterface.TAG_IMAGE_LENGTH,
                        ExifInterface.TAG_IMAGE_WIDTH,
                        ExifInterface.TAG_ISO,
                        ExifInterface.TAG_MAKE,
                        ExifInterface.TAG_MODEL,
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.TAG_SUBSEC_TIME,
                        ExifInterface.TAG_SUBSEC_TIME_DIG,
                        ExifInterface.TAG_SUBSEC_TIME_ORIG,
                        ExifInterface.TAG_WHITE_BALANCE
                };

        ExifInterface newExif = new ExifInterface(newPath);
        for (int i = 0; i < attributes.length; i++) {
            String value = oldExif.getAttribute(attributes[i]);
            if (value != null)
                newExif.setAttribute(attributes[i], value);
        }
        newExif.saveAttributes();
    }

    public static void copyExif(ExifInterface oldExif, String newPath) throws IOException {

        String[] attributes = new String[]
                {
                        ExifInterface.TAG_APERTURE,
                        ExifInterface.TAG_DATETIME,
                        ExifInterface.TAG_DATETIME_DIGITIZED,
                        ExifInterface.TAG_EXPOSURE_TIME,
                        ExifInterface.TAG_FLASH,
                        ExifInterface.TAG_FOCAL_LENGTH,
                        ExifInterface.TAG_GPS_ALTITUDE,
                        ExifInterface.TAG_GPS_ALTITUDE_REF,
                        ExifInterface.TAG_GPS_DATESTAMP,
                        ExifInterface.TAG_GPS_LATITUDE,
                        ExifInterface.TAG_GPS_LATITUDE_REF,
                        ExifInterface.TAG_GPS_LONGITUDE,
                        ExifInterface.TAG_GPS_LONGITUDE_REF,
                        ExifInterface.TAG_GPS_PROCESSING_METHOD,
                        ExifInterface.TAG_GPS_TIMESTAMP,
                        ExifInterface.TAG_IMAGE_LENGTH,
                        ExifInterface.TAG_IMAGE_WIDTH,
                        ExifInterface.TAG_ISO,
                        ExifInterface.TAG_MAKE,
                        ExifInterface.TAG_MODEL,
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.TAG_SUBSEC_TIME,
                        ExifInterface.TAG_SUBSEC_TIME_DIG,
                        ExifInterface.TAG_SUBSEC_TIME_ORIG,
                        ExifInterface.TAG_WHITE_BALANCE
                };

        ExifInterface newExif = new ExifInterface(newPath);
        for (int i = 0; i < attributes.length; i++) {
            String value = oldExif.getAttribute(attributes[i]);
            if (value != null)
                newExif.setAttribute(attributes[i], value);
        }
        newExif.saveAttributes();
    }

    void refreshList() {
        ArrayList<String> clonedPoster = new ArrayList<String>(imageList);
        mPagerAdapter.swap(clonedPoster);
        ArrayList<String> clonedPoster2 = new ArrayList<String>(imageList);
        recyclerViewAdapter.swap(clonedPoster);
        mPagerAdapter.swap(clonedPoster2);
        setActionBarTitle(imageList, currentPosition);

    }

    @Override
    public boolean isPhotoSelected(int position) {
        return currentPosition == position;
    }

    @Override
    public boolean isPhotoSelectionMode() {
        return false;
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        private ArrayList<String> itemList;

        ScreenSlidePagerAdapter(FragmentManager fm, ArrayList<String> itemList) {
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

            if (recyclerViewAdapter.getItemCount() > 0 || recyclerViewAdapter != null) {
                if (itemList.size() > 0) {
                    notifyDataSetChanged();
                }
            }

        }

        //        https://stackoverflow.com/questions/13695649/refresh-images-on-fragmentstatepageradapter-on-resuming-activity
        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }


    private interface ResizeCallback {
        void onResizeSuccess(ArrayList<String> outList);

        void onResizeFailed(String s);
    }

    private class ImageResizeTask extends AsyncTask<ImageResizer, Void, Void> {
        protected Void doInBackground(ImageResizer... imageResizers) {
            int count = imageResizers.length;
            long totalSize = 0;
            for (int i = 0; i < count; i++) {
                imageResizers[i].run();
            }
            return null;
        }

        protected void onProgressUpdate(Void... voids) {

        }

        protected void onPostExecute(Void result) {

        }
    }

    public class ImageResizer {
        private final Context context;
        private final List<String> files;
        private ResizeCallback callback;
        private ArrayList<String> outList;
        OnImageResized onImageResizedCallback = new OnImageResized() {

            @Override
            public void resizeProcessed(ArrayList<String> temp) {
                processFile(temp, onImageResizedCallback);
            }

            @Override
            public void ResizeCompleted(ArrayList<String> outList) {
                if (callback != null) {
                    onDone();
                }
            }
        };

        public ImageResizer(Context context, List<String> files) {
            this.context = context;
            this.files = files;
            this.outList = new ArrayList<String>();
        }


        public void run() {
            processFiles();

        }

        private void processFiles() {
            try {
                ArrayList<String> tempfiles = new ArrayList<String>(files);
                processFile(tempfiles, onImageResizedCallback);
            } catch (Exception e) {
                e.printStackTrace();

            }
        }

        private void processFile(final ArrayList<String> temp, final OnImageResized onImageResized) {
            if (temp.size() == 0) {
                onImageResized.ResizeCompleted(outList);
            } else {
                if ((width != 0 && height != 0) || temp.get(0).toLowerCase().contains("bmp")) {
                    try {
                        URI uri = new URI(temp.get(0));

                        final File imageFile = new File(uri);
                        ImageRequest request = null;
                        if (width != 0 && height != 0) {
                            final BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = true;
                            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
                            float scale = 1.0f;
                            if (options.outWidth > options.outHeight) {
                                scale = (width * 1.0f) / (options.outWidth * 1.0f);
                            } else {
                                scale = (height * 1.0f) / (options.outHeight * 1.0f);
                            }
                            if (scale > 1 || scale <= 0) {
                                scale = 1;
                            }
                            float reqWidth = options.outWidth * scale;
                            float reqHeight = options.outHeight * scale;
                            request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(temp.get(0)))
                                    .setResizeOptions(new ResizeOptions((int) reqWidth, (int) reqHeight))
                                    .build();
                        } else {
                            request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(temp.get(0)))
                                    .build();
                        }
                        ImagePipeline imagePipeline = Fresco.getImagePipeline();
                        DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(request, this);

                        CallerThreadExecutor executor = CallerThreadExecutor.getInstance();
                        dataSource.subscribe(
                                new BaseBitmapDataSubscriber() {
                                    @Override
                                    protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                                        callback.onResizeFailed("Failed to resize at onFailureImpl " + imageFile.getAbsolutePath());
                                    }

                                    @Override
                                    protected void onNewResultImpl(Bitmap bmp) {
                                        ExifInterface exif = null;
                                        try {
                                            if (imageFile.getName().toLowerCase().endsWith("jpg") || imageFile.getName().toLowerCase().endsWith("jpeg")) {
                                                exif = new ExifInterface(imageFile.getAbsolutePath());
                                                exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        try {
                                            Log.d("processFile", "storeImageWithExif " + imageFile);
                                            String outFilePath;
                                            if (exif != null) {
                                                outFilePath = storeImageWithExif(imageFile.getName(), bmp, exif);
                                            } else {
                                                outFilePath = storeImage(imageFile.getParentFile().getAbsolutePath(), imageFile.getName());
                                            }
                                            outList.add(Uri.fromFile(new File(outFilePath)).toString());

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                            callback.onResizeFailed("JSONException " + e.getMessage());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            callback.onResizeFailed("IOException " + e.getMessage());
                                        } catch (URISyntaxException e) {
                                            e.printStackTrace();
                                            callback.onResizeFailed("URISyntaxException " + e.getMessage());
                                        } finally {
                                            temp.remove(0);
                                            onImageResized.resizeProcessed(temp);
                                        }

                                    }
                                }
                                , executor);

                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        callback.onResizeFailed("URISyntaxException " + e.getMessage());
                    }
                } else {

                    try {
                        URI uri = new URI(temp.get(0));
                        File inFile = new File(uri);
                        Log.d("processFile", "storeImage " + uri);
                        String outFilePath = storeImage(inFile.getParentFile().getAbsolutePath(), inFile.getName());
                        outList.add(Uri.fromFile(new File(outFilePath)).toString());
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        callback.onResizeFailed("URISyntaxException storeImage " + e.getMessage());
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callback.onResizeFailed("JSONException storeImage " + e.getMessage());
                    } catch (IOException e) {
                        e.printStackTrace();
                        callback.onResizeFailed("IOException storeImage " + e.getMessage());
                    } finally {
                        temp.remove(0);
                        onImageResized.resizeProcessed(temp);
                    }


                }
            }
        }

        private void onDone() {
            try {
                if (callback != null) {
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onResizeSuccess(outList);
                        }
                    });
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }


        public void setCallback(ResizeCallback callback) {
            this.callback = callback;
        }


    }

    private interface OnImageResized {
        void resizeProcessed(ArrayList<String> temp);

        void ResizeCompleted(ArrayList<String> outList);
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

}
