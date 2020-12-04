package com.example.gradleplugin;

import android.util.Log;

public class LogUtil {
    private static final String TAG = "Debounce";

    public static void d(String msg) {
        Log.d(TAG, msg);
    }
}
