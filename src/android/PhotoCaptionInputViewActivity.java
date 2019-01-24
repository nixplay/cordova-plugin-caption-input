
package com.creedon.cordova.plugin.captioninput;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
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
import android.media.MediaMetadataRetriever;
import android.net.Uri;
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
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.view.View.GONE;
import static com.creedon.cordova.plugin.captioninput.Constants.KEY_CAPTIONS;
import static com.creedon.cordova.plugin.captioninput.Constants.KEY_IMAGES;
import static com.creedon.cordova.plugin.captioninput.Constants.KEY_INVALIDIMAGES;
import static com.creedon.cordova.plugin.captioninput.Constants.KEY_METADATAS;
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
        Boolean first = true;

        public void onPageScrollStateChanged(int state) {
        }

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            mPagerAdapter.stopVideoPlayback(position);

            if (first && positionOffset == 0 && positionOffsetPixels == 0) {
                onPageSelected(0);
                first = false;
            } else {
                onPageSelected(position);
            }
        }

        public void onPageSelected(int position) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    currentPosition = mPager.getCurrentItem();
                    mPagerAdapter.stopVideoPlayback(currentPosition);
                    setActionBarTitle(imageList, currentPosition);
                    if (isVideo(imageList.get(currentPosition))) {
                        mEditText.setText("");
                        mEditText.setVisibility(View.INVISIBLE);
                    } else {
                        mEditText.setText(captions.get(currentPosition));
                        mEditText.setVisibility(View.VISIBLE);
                    }
                    linearLayoutManager.scrollToPositionWithOffset(currentPosition, -1);
                    if (recyclerViewAdapter != null) {
                        recyclerViewAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    };
    private ViewPager.OnAdapterChangeListener onAdapterChangeListener = new ViewPager.OnAdapterChangeListener() {
        @Override
        public void onAdapterChanged(@NonNull ViewPager viewPager, @Nullable PagerAdapter oldAdapter, @Nullable PagerAdapter newAdapter) {

        }
    };
    private ArrayList<String> preSelectedAssets = new ArrayList<String>();
    private int maxImages;
    private ImageResizer imageResizer;

    private List<String> currentTaskIDs = Collections.synchronizedList(new ArrayList());


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
        try {
            isOptimizeSize = sharedPrefs.getBoolean("nixSettings.settings.resolution", false);
        } catch (Exception e) {
            try {
                String ret = sharedPrefs.getString("nixSettings.settings.resolution", "");
                isOptimizeSize = Boolean.parseBoolean(ret);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        if (isOptimizeSize) {
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
                        Log.d(TAG, " TextView " + v.toString() + " actionId " + actionId + " event " + event);
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
//content uri all the way
                        for (String s : preSelectedAssets) {
                            serialPreselectedAssets.add(Uri.parse(s));
                        }

                        Matisse.from(PhotoCaptionInputViewActivity.this)
                                .choose(MimeType.of(
                                        MimeType.JPEG,
                                        MimeType.PNG,
                                        MimeType.MP4
                                ), false)
                                .countable(true)
                                .maxSelectable(PhotoCaptionInputViewActivity.this.maxImages)
                                .gridExpectedSize((int) convertDpToPixel(120, PhotoCaptionInputViewActivity.this))
                                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                                .thumbnailScale(0.85f)
                                .imageEngine(new GlideEngine())
                                .enablePreview(false)
                                .showUseOrigin(false)
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

    public static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            cursor.close();
            return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    private static boolean isVideo(String filePath) {
        filePath = filePath.toLowerCase();
        return filePath.endsWith(".mp4");
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
//                preSelectedAssets.remove(currentPosition);
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

        for(String currentTaskID : currentTaskIDs) {
            BackgroundExecutor.cancelAll(currentTaskID, true);
        }
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
                    assert imm != null;
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
        } catch (Exception e) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CHOOSE) {
                ArrayList<String> newImages = new ArrayList<String>();
                ArrayList<String> newPreselectedAssets = new ArrayList<String>();
                ArrayList<String> newCaptions = new ArrayList<String>();

                List<Uri> result = Matisse.obtainResult(data);
                List<String> fileActualPaths = Matisse.obtainPathResult(data);
                for (int i = 0; i < result.size(); i++) {
                    String fileActualPath = fileActualPaths.get(i);
                    String uriString = result.get(i).toString();
                    int index = preSelectedAssets.indexOf(uriString);
                    if (fileActualPath.contains("file:///")) {
                        newImages.add(fileActualPath);
                    } else {
                        newImages.add(Uri.fromFile(new File(fileActualPath)).toString());
                    }

                    newPreselectedAssets.add(uriString);
                    if (preSelectedAssets.contains(uriString)) {
                        if (index > 0 && index < captions.size()) {
                            newCaptions.add(captions.get(index));
                        } else {
                            newCaptions.add("");
                        }
                    } else {
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
                        .setStyle(KProgressHUD.Style.PIE_DETERMINATE)
                        .setDetailsLabel("")
                        .setCancellable(false)
                        .setAnimationSpeed(2)
                        .setDimAmount(0.5f)
                        .setMaxProgress(imageList.size())
                        .show();

            }
        });
        for(int i = 0 ; i < imageList.size() ; i++) {
            String currentTaskID = String.valueOf(i);
            Log.d("currentTaskIDs.add ", currentTaskID);
            currentTaskIDs.add(currentTaskID);
        }
        imageResizer = new ImageResizer(this, imageList, new OnResizedCallback() {
            //https://stackoverflow.com/questions/7860822/sorting-hashmap-based-on-keys
            TreeMap<Integer, String> outList = new TreeMap<Integer, String>();
            TreeMap<Integer, JSONObject> metaDatas = new TreeMap<Integer, JSONObject>();


            @Override
            public void onResizeSuccess(String result, Integer index, String originalFilename, JSONObject metaData) {
                outList.put(index, result);
                metaDatas.put(index, ((metaData == null) ? new JSONObject() : metaData));
                final int process = outList.size();
//                Log.d(TAG,"onResizeSuccess "+index+": result-> "+result+ " originalFilename : "+originalFilename);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        kProgressHUD.setProgress(process);
                    }
                });

                if (outList.size() == imageList.size()) {
                    kProgressHUD.dismiss();

                    Bundle conData = new Bundle();
                    try {
                        JSONObject jsonObject = new JSONObject();
                        //https://stackoverflow.com/questions/15402321/how-to-convert-hashmap-to-json-array-in-android
                        Collection<String> values = outList.values();

                        Collection<JSONObject> jsonMetaDataValues = metaDatas.values();

                        JSONArray array = new JSONArray(values);
                        JSONArray jsonMetaDataArray = new JSONArray(jsonMetaDataValues);
                        jsonObject.put(KEY_IMAGES, array);
                        jsonObject.put(KEY_CAPTIONS, new JSONArray(captions));
                        jsonObject.put(KEY_PRESELECTS, new JSONArray());
                        jsonObject.put(KEY_INVALIDIMAGES, new JSONArray());
                        jsonObject.put(KEY_TYPE, type);
                        jsonObject.put(KEY_METADATAS, jsonMetaDataArray);

                        conData.putString(Constants.RESULT, jsonObject.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
//                    for(String currentTaskID : currentTaskIDs) {
//                        BackgroundExecutor.cancelAll(currentTaskID, true);
//                    }
                    currentTaskIDs.clear();
                    Intent intent = new Intent();
                    intent.putExtras(conData);
                    setResult(RESULT_OK, intent);
                    finishActivity(Constants.REQUEST_SUBMIT);
                    finish();

                }
            }

            @Override
            public void onResizePrecess(final Integer process) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        kProgressHUD.setProgress(process);
//                    }
//                });

            }

            @Override
            public void onResizeFailed(String s) {
                Log.e(TAG, s);
            }
        });


        for (int i = 0; i < imageList.size(); i++) {

            final int nextIndex = i;
            final String fileName = imageList.get(nextIndex).toLowerCase();

            BackgroundExecutor.execute(
                    new BackgroundExecutor.Task(currentTaskIDs.get(i), 0L, fileName) {
                        @Override
                        public void execute() {
                            try {
                                imageResizer.processFile(imageList.get(nextIndex), nextIndex);
                            } catch (final Throwable e) {
                                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                            }
                        }
                    }
            );
        }


    }


    protected String storeImage(String inFilePath, String inFileName) throws JSONException, URISyntaxException {


        String outFilePath = System.getProperty("java.io.tmpdir") + "/";
        if (!(inFilePath + File.separator + inFileName).equals(outFilePath + inFileName)) {
            copyFile(inFilePath + File.separator, inFileName, outFilePath);
            try {
                if (inFilePath.toLowerCase().endsWith("jpeg") || inFilePath.toLowerCase().endsWith("jpg")) {
                    copyExif(inFilePath + File.separator + inFileName, outFilePath + inFileName);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return outFilePath + inFileName;
        }
        return inFilePath + File.separator + inFileName;

    }

    protected String storeImage(String inFileName, Bitmap bmp) throws JSONException, IOException, URISyntaxException {

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
        return filePath;

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
            Log.e(TAG, fnfe1.getMessage());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
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
        private SparseArray<WeakReference<ScreenSlidePageFragment>> mPageReferenceMap = new SparseArray<WeakReference<ScreenSlidePageFragment>>();

        ScreenSlidePagerAdapter(FragmentManager fm, ArrayList<String> itemList) {
            super(fm);
            this.itemList = itemList;
        }

        @Override
        public Fragment getItem(int position) {
            return getFragment(position);
        }

        @NonNull
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            ScreenSlidePageFragment myFragment = ScreenSlidePageFragment.newInstance(itemList.get(position));
            mPageReferenceMap.put(position, new WeakReference<ScreenSlidePageFragment>(myFragment));

            return super.instantiateItem(container, position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            mPageReferenceMap.remove(position);
        }

        public ScreenSlidePageFragment getFragment(int position) {
            WeakReference<ScreenSlidePageFragment> weakReference = mPageReferenceMap.get(position);

            if (null != weakReference) {
                return (ScreenSlidePageFragment) weakReference.get();
            } else {
                return null;
            }
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

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        public void stopVideoPlayback(int position) {
            ScreenSlidePageFragment fragment = getFragment(position);
            if (fragment != null) {
                fragment.stopVideoPlayback();
            }
        }
    }


    private interface OnResizedCallback {
        void onResizeSuccess(String result, Integer index, String originalFilename, @Nullable JSONObject metaData);

        void onResizePrecess(Integer process);

        void onResizeFailed(String s);
    }


    public class ImageResizer {
        private final Context context;

        private OnResizedCallback callback;
        private boolean isCancel;


        public ImageResizer(Context context, OnResizedCallback callback) {
            this.context = context;

            this.callback = callback;

        }


        public void processFile(String fileName, Integer index) {
            if (isCancel) {
                if (callback != null) callback.onResizeFailed("User cancelled");
                return;
            }


            //fixed http://crashes.to/s/d00290ba305 stackoverflow

            Uri src = Uri.parse(fileName);
            if ((width != 0 && height != 0) || MimeType.BMP.checkType(getContentResolver(), src)) {
                try {
                    URI uri = new URI(fileName);

                    final File imageFile = new File(uri);
                    ImageRequest request;
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
                        request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(fileName))
                                .setResizeOptions(new ResizeOptions((int) reqWidth, (int) reqHeight))
                                .build();
                    } else {
                        request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(fileName))
                                .build();
                    }
                    ImagePipeline imagePipeline = Fresco.getImagePipeline();
                    DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(request, this);

                    CallerThreadExecutor executor = CallerThreadExecutor.getInstance();
                    dataSource.subscribe(new Subscriber(callback, index, imageFile), executor);

                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    if (callback != null) callback.onResizeFailed("URISyntaxException " + e.getMessage());
                }
            } else if (MimeType.MP4.checkType(getContentResolver(), src)) {
                JSONObject metaData = new JSONObject();
                try {

                    MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                    mediaMetadataRetriever.setDataSource(PhotoCaptionInputViewActivity.this, src);
//                        Bitmap previewBitmap = mediaMetadataRetriever.getFrameAtTime(0);
//                        Bitmap thumbnailBitmap = (previewBitmap.getWidth() >= previewBitmap.getHeight()) ?
//                                Bitmap.createBitmap(
//                                        previewBitmap,
//                                        previewBitmap.getWidth() / 2 - previewBitmap.getHeight() / 2,
//                                        0,
//                                        previewBitmap.getHeight(),
//                                        previewBitmap.getHeight()
//                                ) :
//                                Bitmap.createBitmap(
//                                        previewBitmap,
//                                        0,
//                                        previewBitmap.getHeight() / 2 - previewBitmap.getWidth() / 2,
//                                        previewBitmap.getWidth(),
//                                        previewBitmap.getWidth()
//                                );
//
//                        originalFileName = temp.get(0).substring(temp.get(0).lastIndexOf("/") + 1);
//                        subfileName = temp.get(0).substring(temp.get(0).lastIndexOf(".") + 1);
//                        previewBitmapFileName = originalFileName.replace(subfileName, "-preview.jpg");
//                        thumbnailBitmapFileName = originalFileName.replace(subfileName, "-thumbnail.jpg");
//                        String previewBitmapFilePath = storeImage(previewBitmapFileName, previewBitmap);
//                        String thumbnailBitmapFilePath = storeImage(thumbnailBitmapFileName, thumbnailBitmap);
//                        Log.d("previewBitmapFilePath", previewBitmapFilePath);
//                        Log.d("thumbnailBitmapFilePath", thumbnailBitmapFilePath);


                    long durationMs = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    long videoWidth = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                    long videoHeight = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));

                    metaData.put("start", 0L);
                    metaData.put("duration", 15000L > durationMs ? durationMs : 15000L);
                    metaData.put("width", videoWidth);
                    metaData.put("height", videoHeight);
                    metaData.put("originalDuration", durationMs);
                    metaData.put("edited", "auto");


                } catch (JSONException e) {
                    e.printStackTrace();
                    if (callback != null) callback.onResizeFailed("JSONException storeImage " + e.getMessage());
                }
                if (callback != null) {
                    callback.onResizeSuccess(Uri.fromFile(new File(fileName)).toString(), index, fileName, metaData);
                    callback.onResizePrecess(index + 1);
                }


            } else if (MimeType.JPEG.checkType(getContentResolver(), src) || MimeType.PNG.checkType(getContentResolver(), src)) {
                String outFilePath = "";
                try {
                    URI uri = new URI(fileName);
                    File inFile = new File(uri);
                    outFilePath = storeImage(inFile.getParentFile().getAbsolutePath(), inFile.getName());


                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    if (callback != null) callback.onResizeFailed("URISyntaxException storeImage " + e.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                    if (callback != null) callback.onResizeFailed("JSONException storeImage " + e.getMessage());
                }
                if (callback != null) {
                    callback.onResizeSuccess(Uri.fromFile(new File(outFilePath)).toString(), index, fileName, null);
                    callback.onResizePrecess(index);
                }


            } else {
                if (callback != null){
                    callback.onResizeSuccess("", index, fileName, null);
                    callback.onResizePrecess(index + 1);
                }
            }

        }

        public void cancel() {
            isCancel = true;
        }
    }

    class Subscriber extends BaseBitmapDataSubscriber {
        private OnResizedCallback callback;
        private Integer index;
        private File imageFile;

        public Subscriber(OnResizedCallback callback, Integer index, File imageFile) {
            this.callback = callback;
            this.index = index;
            this.imageFile = imageFile;
        }

        @Override
        public void onProgressUpdate(DataSource<CloseableReference<CloseableImage>> dataSource) {
            super.onProgressUpdate(dataSource);
        }

        @Override
        protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
            if (callback != null) callback.onResizeFailed("Failed to resize at onFailureImpl " + imageFile.getAbsolutePath());
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
            String outFilePath = "";
            try {
//                Log.d("processFile", "storeImageWithExif " + imageFile);

                if (exif != null) {
                    outFilePath = storeImageWithExif(imageFile.getName(), bmp, exif);
                } else {
                    outFilePath = storeImage(imageFile.getParentFile().getAbsolutePath(), imageFile.getName());
                }


            } catch (JSONException e) {
                e.printStackTrace();
                if (callback != null) callback.onResizeFailed("JSONException " + e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                if (callback != null) callback.onResizeFailed("IOException " + e.getMessage());
            } catch (URISyntaxException e) {
                e.printStackTrace();
                if (callback != null) callback.onResizeFailed("URISyntaxException " + e.getMessage());
            }
            if (callback != null) {
                callback.onResizeSuccess(Uri.fromFile(new File(outFilePath)).toString(), index, imageFile.getAbsolutePath(), null);
                callback.onResizePrecess(index);
            }
        }
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    static final class BackgroundExecutor {


        public static final Executor DEFAULT_EXECUTOR = Executors.newScheduledThreadPool(2 * Runtime.getRuntime().availableProcessors());
        private static Executor executor = DEFAULT_EXECUTOR;

        public static List<Task> getTASKS() {
            return TASKS;
        }

        private static final List<Task> TASKS = new ArrayList<Task>();
        private static final ThreadLocal<String> CURRENT_SERIAL = new ThreadLocal<String>();

        private BackgroundExecutor() {
        }

        /**
         * Execute a runnable after the given delay.
         *
         * @param runnable the task to execute
         * @param delay    the time from now to delay execution, in milliseconds
         *                 <p>
         *                 if <code>delay</code> is strictly positive and the current
         *                 executor does not support scheduling (if
         *                 Executor has been called with such an
         *                 executor)
         * @return Future associated to the running task
         * @throws IllegalArgumentException if the current executor set by Executor
         *                                  does not support scheduling
         */
        private static Future<?> directExecute(Runnable runnable, long delay) {
            Future<?> future = null;
            if (delay > 0) {
            /* no serial, but a delay: schedule the task */
                if (!(executor instanceof ScheduledExecutorService)) {
                    throw new IllegalArgumentException("The executor set does not support scheduling");
                }
                ScheduledExecutorService scheduledExecutorService = (ScheduledExecutorService) executor;
                future = scheduledExecutorService.schedule(runnable, delay, TimeUnit.MILLISECONDS);
            } else {
                if (executor instanceof ExecutorService) {
                    ExecutorService executorService = (ExecutorService) executor;
                    future = executorService.submit(runnable);
                } else {
                /* non-cancellable task */
                    executor.execute(runnable);
                }
            }
            return future;
        }

        /**
         * Execute a task after (at least) its delay <strong>and</strong> after all
         * tasks added with the same non-null <code>serial</code> (if any) have
         * completed execution.
         *
         * @param task the task to execute
         * @throws IllegalArgumentException if <code>task.delay</code> is strictly positive and the
         *                                  current executor does not support scheduling (if
         *                                  Executor has been called with such an
         *                                  executor)
         */
        public static synchronized void execute(Task task) {
            Future<?> future = null;
            if (task.serial == null || !hasSerialRunning(task.serial)) {
                task.executionAsked = true;
                future = directExecute(task, task.remainingDelay);
            }
            if ((task.id != null || task.serial != null) && !task.managed.get()) {
            /* keep task */
                task.future = future;
                TASKS.add(task);
            }
        }

        /**
         * Indicates whether a task with the specified <code>serial</code> has been
         * submitted to the executor.
         *
         * @param serial the serial queue
         * @return <code>true</code> if such a task has been submitted,
         * <code>false</code> otherwise
         */
        private static boolean hasSerialRunning(String serial) {
            for (Task task : TASKS) {
                if (task.executionAsked && serial.equals(task.serial)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Retrieve and remove the first task having the specified
         * <code>serial</code> (if any).
         *
         * @param serial the serial queue
         * @return task if found, <code>null</code> otherwise
         */
        private static Task take(String serial) {
            int len = TASKS.size();
            for (int i = 0; i < len; i++) {
                if (serial.equals(TASKS.get(i).serial)) {
                    return TASKS.remove(i);
                }
            }
            return null;
        }

        /**
         * Cancel all tasks having the specified <code>id</code>.
         *
         * @param id                    the cancellation identifier
         * @param mayInterruptIfRunning <code>true</code> if the thread executing this task should be
         *                              interrupted; otherwise, in-progress tasks are allowed to
         *                              complete
         */
        public static synchronized void cancelAll(String id, boolean mayInterruptIfRunning) {
            for (int i = TASKS.size() - 1; i >= 0; i--) {
                Task task = TASKS.get(i);
                if (id.equals(task.id)) {
                    if (task.future != null) {
                        task.future.cancel(mayInterruptIfRunning);
                        if (!task.managed.getAndSet(true)) {
                        /*
                         * the task has been submitted to the executor, but its
                         * execution has not started yet, so that its run()
                         * method will never call postExecute()
                         */
                            task.postExecute();
                        }
                    } else if (task.executionAsked) {
                        Log.w(TAG, "A task with id " + task.id + " cannot be cancelled (the executor set does not support it)");
                    } else {
                    /* this task has not been submitted to the executor */
                        TASKS.remove(i);
                    }
                }
            }

        }

        public static abstract class Task implements Runnable {

            private String id;
            private long remainingDelay;
            private long targetTimeMillis; /* since epoch */
            private String serial;
            private boolean executionAsked;
            private Future<?> future;

            /*
             * A task can be cancelled after it has been submitted to the executor
             * but before its run() method is called. In that case, run() will never
             * be called, hence neither will postExecute(): the tasks with the same
             * serial identifier (if any) will never be submitted.
             *
             * Therefore, cancelAll() *must* call postExecute() if run() is not
             * started.
             *
             * This flag guarantees that either cancelAll() or run() manages this
             * task post execution, but not both.
             */
            private AtomicBoolean managed = new AtomicBoolean();

            public Task(String id, long delay, String serial) {
                if (!"".equals(id)) {
                    this.id = id;
                }
                if (delay > 0) {
                    remainingDelay = delay;
                    targetTimeMillis = System.currentTimeMillis() + delay;
                }
                if (!"".equals(serial)) {
                    this.serial = serial;
                }
            }

            @Override
            public void run() {
                if (managed.getAndSet(true)) {
                /* cancelled and postExecute() already called */
                    return;
                }

                try {
                    CURRENT_SERIAL.set(serial);
                    execute();
                } finally {
                /* handle next tasks */
                    postExecute();
                }
            }

            public abstract void execute();

            private void postExecute() {
                if (id == null && serial == null) {
                /* nothing to do */
                    return;
                }
                CURRENT_SERIAL.set(null);
                synchronized (BackgroundExecutor.class) {
                /* execution complete */
                    TASKS.remove(this);

                    if (serial != null) {
                        Task next = take(serial);
                        if (next != null) {
                            if (next.remainingDelay != 0) {
                            /* the delay may not have elapsed yet */
                                next.remainingDelay = Math.max(0L, targetTimeMillis - System.currentTimeMillis());
                            }
                        /* a task having the same serial was queued, execute it */
                            BackgroundExecutor.execute(next);
                        }
                    }
                }
            }
        }
    }
}


