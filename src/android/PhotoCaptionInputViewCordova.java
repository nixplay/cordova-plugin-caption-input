package com.creedon.cordova.plugin.captioninput;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class PhotoCaptionInputViewCordova extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("showCaptionInput")) {
            JSONObject options = args.getJSONObject(0);
            this.showCaptionInput(options, callbackContext);
            return true;
        }
        return false;
    }

    private void showCaptionInput(JSONObject options, CallbackContext callbackContext) {
        if (options != null && options.length() > 0) {

            callbackContext.success(options);
            android.app.FragmentManager fm = this.cordova.getActivity().getFragmentManager();
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager) this.cordova.getActivity().getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(mi);
            long totalMegs = mi.totalMem / 1048576L;
            System.out.println("[NIX] totalMegs: " + totalMegs);

            Intent intent = new Intent(cordova.getActivity(), PhotoCaptionInputViewActivity.class);

            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra("options",options.toString());
            this.cordova.startActivityForResult(this, intent, 0);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }
}
