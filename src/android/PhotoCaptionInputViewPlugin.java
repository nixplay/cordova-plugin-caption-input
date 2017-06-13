package com.creedon.cordova.plugin.captioninput;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static org.apache.cordova.PluginResult.Status.OK;

/**
 * This class echoes a string called from JavaScript.
 */
public class PhotoCaptionInputViewPlugin extends CordovaPlugin {

    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        if (action.equals("showCaptionInput")) {
            JSONObject options = args.getJSONObject(0);
            this.showCaptionInput(options, callbackContext);
            return true;
        }
        return false;
    }

    private void showCaptionInput(JSONObject options, CallbackContext callbackContext) {
        if (options != null && options.length() > 0) {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager) this.cordova.getActivity().getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(mi);
            long totalMegs = mi.totalMem / 1048576L;
            System.out.println("[NIX] totalMegs: " + totalMegs);

            Intent intent = new Intent(cordova.getActivity(), PhotoCaptionInputViewActivity.class);

            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra("options", options.toString());
            this.cordova.startActivityForResult(this, intent, 0);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
//            if(requestCode == REQUEST_SUBMIT){

            String result = data.getStringExtra(Constants.RESULT);
            JSONObject res = null;
            try {
                res = new JSONObject(result);
                this.callbackContext.success(res);
                PluginResult pluginResult = new PluginResult(OK, res);
                pluginResult.setKeepCallback(false);
                this.callbackContext.sendPluginResult(pluginResult);
            } catch (JSONException e) {
                e.printStackTrace();
                res = new JSONObject();
                if (this.callbackContext != null) {
                    this.callbackContext.error(res);
                }

            }

//            }else {
//                JSONObject res = new JSONObject();
//                if(this.callbackContext != null ) {
//                    this.callbackContext.error(res);
//                }
//            }

        } else if (resultCode == Activity.RESULT_CANCELED && data != null) {
            String error = data.getStringExtra("ERRORMESSAGE");
            if (error == null)
                this.callbackContext.error("Error");
            this.callbackContext.error(error);
        } else if (resultCode == Activity.RESULT_CANCELED) {
            JSONObject res = new JSONObject();
            if (this.callbackContext != null)
                this.callbackContext.error(res);

        } else {
            JSONObject res = new JSONObject();
            if (this.callbackContext != null)
                this.callbackContext.error(res);
        }

    }
}
