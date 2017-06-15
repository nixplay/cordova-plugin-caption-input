package com.creedon.cordova.plugin.captioninput;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
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
import com.kbeanie.multipicker.api.CacheLocation;
import com.kbeanie.multipicker.api.ImagePicker;
import com.kbeanie.multipicker.api.Picker;
import com.kbeanie.multipicker.api.callbacks.ImagePickerCallback;
import com.kbeanie.multipicker.api.entity.ChosenImage;
import com.rengwuxian.materialedittext.MaterialEditText;

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

import it.sephiroth.android.library.exif2.ExifInterface;
import it.sephiroth.android.library.exif2.ExifTag;

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
    private FakeR fakeR;
    private ArrayList<String> captions;
    private int currentPosition;
    private ViewPager mPager;
    private ScreenSlidePagerAdapter mPagerAdapter;
    private MaterialEditText mEditText;
    private LinearLayoutManager linearLayoutManager;
    private RecyclerViewAdapter recyclerViewAdapter;
    private ImagePicker imagePicker;
    private KProgressHUD kProgressHUD;
    private ArrayList<String> imageList;
    private int width, height;


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
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
                this.width = jsonObject.has("width") ? jsonObject.getInt("width") : 0;
                this.height = jsonObject.has("height") ? jsonObject.getInt("height") : 0;
//                this.quality = jsonObject.getInt("quality");
                JSONArray imagesJsonArray = jsonObject.getJSONArray("images");
//                JSONArray preSelectedAssets = jsonObject.getJSONArray("preSelectedAssets");
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
                        if (jsonObj.contains("content://com.android.providers.media.documents")) {
                            //get real path
                            //https://stackoverflow.com/questions/20067508/get-real-path-from-uri-android-kitkat-new-storage-access-framework

                            stringArray.add(Uri.fromFile(new File(getPath(PhotoCaptionInputViewActivity.this, Uri.parse(jsonObj)))).toString());
//                            stringArray.add(Uri.fromFile(new File().toString());
                        } else {
                            stringArray.add(jsonObj);
                        }
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
                (findViewById(fakeR.getId("id", "btnSubmit"))).setOnClickListener(new View.OnClickListener() {
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

//                        TODO James Kong 2017-06-09 IDEAL image picker with replace with existing
                        imagePicker = new ImagePicker(PhotoCaptionInputViewActivity.this);
                        imagePicker.setImagePickerCallback(new ImagePickerCallback() {
                            @Override
                            public void onImagesChosen(List<ChosenImage> images) {
                                if (kProgressHUD != null) {
                                    kProgressHUD.dismiss();
                                }
                                for (ChosenImage file : images) {
                                    String uriString = file.getQueryUri();
                                    imageList.add(Uri.fromFile(new File(getPath(PhotoCaptionInputViewActivity.this, Uri.parse(uriString)))).toString());
//                                    imageList.add(Uri.fromFile(new File(file.getOriginalPath())).toString());
                                    captions.add("");
                                }
                                refreshList();
                            }

                            @Override
                            public void onError(String message) {
                                // Do error handling
                                Log.d(TAG, message);
                            }
                        });

                        if (PhotoCaptionInputViewActivity.this.width > 0 && PhotoCaptionInputViewActivity.this.height > 0) {
                            imagePicker.ensureMaxSize(PhotoCaptionInputViewActivity.this.width, PhotoCaptionInputViewActivity.this.height);
                        }
                        imagePicker.allowMultiple(); // Default is false
                        imagePicker.shouldGenerateMetadata(true);
                        imagePicker.setCacheLocation(CacheLocation.INTERNAL_APP_DIR);
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
                actionBar.setHomeAsUpIndicator(fakeR.getId("drawable", "ic_up_white_24dp"));
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
                captions.remove(currentPosition);
                currentPosition = Math.max(0, Math.min(currentPosition, imageList.size() - 1));
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
        recyclerViewAdapter.notifyDataSetChanged();

    }

    @Override
    public void onItemLongClick(View view, int position) {
        currentPosition = position;
        mPager.setCurrentItem(position);
        setActionBarTitle(imageList, currentPosition);
        mEditText.setText(captions.get(currentPosition));
        Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        v.vibrate(500);
        recyclerViewAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == Picker.PICK_IMAGE_DEVICE) {
                kProgressHUD = KProgressHUD.create(PhotoCaptionInputViewActivity.this)
                        .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                        .setDetailsLabel(getString(fakeR.getId("string", "DOWNLOADING")))
                        .setCancellable(false)
                        .setAnimationSpeed(2)
                        .setDimAmount(0.5f)
                        .show();

                imagePicker.submit(data);
            }
        }

    }

    public void setActionBarTitle(ArrayList<String> actionBarTitle, int index) {
        try {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle((index + 1) + "/" + actionBarTitle.size());
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void finishWithResult() throws JSONException {
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
        if (kProgressHUD != null) {
            if (kProgressHUD.isShowing()) {
                kProgressHUD.dismiss();
            }
        }
        kProgressHUD = KProgressHUD.create(PhotoCaptionInputViewActivity.this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setDetailsLabel(getString(fakeR.getId("string", "LOADING")))
                .setCancellable(false)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f)
                .show();

        ArrayList<String> outList = new ArrayList<String>();
        resizeImage(imageList, outList, new ResizeCallback() {

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

            }
        });

    }

    private void resizeImage(final ArrayList<String> imageList, final ArrayList<String> outList, final ResizeCallback resizeCallback) {

        if (imageList.size() == 0) {
            resizeCallback.onResizeSuccess(outList);
        } else {
            if (this.width != 0 && this.height != 0) {
                try {
                    URI uri = new URI(imageList.get(0));

                    final File imageFile = new File(uri);
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
                    ImageRequest request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(imageList.get(0)))
                            .setResizeOptions(new ResizeOptions((int) reqWidth, (int) reqHeight))
                            .build();
                    ImagePipeline imagePipeline = Fresco.getImagePipeline();
                    final DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(request, this);

                    CallerThreadExecutor executor = CallerThreadExecutor.getInstance();
                    dataSource.subscribe(
                            new BaseBitmapDataSubscriber() {
                                @Override
                                protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                                    resizeCallback.onResizeFailed("Failed to resize at onFailureImpl " + imageFile.getAbsolutePath());
                                }

                                @Override
                                protected void onNewResultImpl(Bitmap bmp) {
                                    ExifInterface exif = new ExifInterface();

                                    try {
                                        exif.readExif(imageFile.getAbsolutePath(), ExifInterface.Options.OPTION_ALL);
                                        ExifTag orientationTag = exif.getTag(ExifInterface.TAG_ORIENTATION);
                                        long orientation = orientationTag.getValueAsLong(0);

                                        Log.d(TAG, "orientationTag " + orientationTag.toString());

                                        Log.d(TAG, "orientation " + orientation);
                                    } catch (Exception e) {
                                        Log.e(TAG, "exif.readExif( " + imageFile.getAbsolutePath() + " , ExifInterface.Options.OPTION_ALL )");
                                        resizeCallback.onResizeFailed("exif.readExif( " + imageFile.getAbsolutePath() + " , ExifInterface.Options.OPTION_ALL )");
                                    }
                                    try {
                                        exif.setTagValue(ExifInterface.TAG_ORIENTATION, 1);

                                    } catch (Exception e) {
                                        Log.e(TAG, "exif.setTagValue(ExifInterface.TAG_ORIENTATION,1)");
                                    }
                                    try {
                                        String outFilePath = storeImageWithExif(imageFile.getName(), bmp, exif);
                                        outList.add(Uri.fromFile(new File(outFilePath)).toString());
                                        imageList.remove(0);
                                        resizeImage(imageList, outList, resizeCallback);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        resizeCallback.onResizeFailed("JSONException " + e.getMessage());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        resizeCallback.onResizeFailed("IOException " + e.getMessage());
                                    } catch (URISyntaxException e) {
                                        e.printStackTrace();
                                        resizeCallback.onResizeFailed("URISyntaxException " + e.getMessage());
                                    }

                                }
                            }
                            , executor);

                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    resizeCallback.onResizeFailed("URISyntaxException " + e.getMessage());
                }
            } else {

                try {
                    URI uri = new URI(imageList.get(0));
                    final File inFile = new File(uri);
                    String outFilePath = storeImage(inFile.getParentFile().getAbsolutePath(), inFile.getName());
                    outList.add(Uri.fromFile(new File(outFilePath)).toString());
                    imageList.remove(0);
                    resizeImage(imageList, outList, resizeCallback);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    resizeCallback.onResizeFailed("URISyntaxException storeImage " + e.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                    resizeCallback.onResizeFailed("JSONException storeImage " + e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                    resizeCallback.onResizeFailed("IOException storeImage " + e.getMessage());
                }


            }

        }
    }

    protected String storeImage(String inFilePath, String inFileName) throws JSONException, IOException, URISyntaxException {


        String outFilePath = System.getProperty("java.io.tmpdir") + "/";
        copyFile(inFilePath + File.separator, inFileName, outFilePath);
        return outFilePath + inFileName;

    }

    protected String storeImageWithExif(String inFileName, Bitmap bmp, ExifInterface exif) throws JSONException, IOException, URISyntaxException {

        String filename = inFileName;
        String filePath = System.getProperty("java.io.tmpdir") + "/" + filename;
        exif.writeExif(bmp, filePath, 100);
        return filePath;

    }

    //James Kong 2017-01-27
    protected String storeImageWithExif(String inFilePath, String infileName) throws JSONException, IOException, URISyntaxException {
        String outFilePath = System.getProperty("java.io.tmpdir") + "/";
        ExifInterface exif = new ExifInterface();
        try {
            exif.readExif(inFilePath + infileName, ExifInterface.Options.OPTION_ALL);
        } catch (Exception e) {
            e.printStackTrace();
        }
        copyFile(inFilePath + File.separator, infileName, outFilePath);
        exif.writeExif(outFilePath + infileName);
        return outFilePath;

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
            recyclerViewAdapter.notifyDataSetChanged();
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


    private interface ResizeCallback {
        void onResizeSuccess(ArrayList<String> outList);

        void onResizeFailed(String s);
    }
}
