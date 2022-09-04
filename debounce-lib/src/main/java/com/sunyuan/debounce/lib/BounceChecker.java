package com.sunyuan.debounce.lib;

import android.os.SystemClock;
import android.util.Log;

/**
 * author : Sy007
 * date   : 2020/11/29
 * desc   : 注入类
 * version: 1.0
 * <p>
 * 这两个字段变量的值在编译时根据build.gradle配置动态改变
 * <p>
 * {@link BounceChecker#sCheckTime}
 * {@link BounceChecker#sDebug}
 * <p>
 */
public class BounceChecker {

    private static long sCheckTime = 1000L;
    private static boolean sDebug = true;

    private long lastClickTime;

    /**
     * 判断是否是快速点击
     */
    public boolean check() {
        boolean isBounce = SystemClock.elapsedRealtime() - lastClickTime < sCheckTime;
        if (sDebug) {
            Log.d("BounceChecker", "[checkTime:" + sCheckTime + ",isBounce:" + isBounce + "]");
        }
        if (!isBounce) {
            lastClickTime = SystemClock.elapsedRealtime();
        }
        return isBounce;
    }
}
