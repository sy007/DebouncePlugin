package com.sunyuan.debounce.lib;

import android.os.SystemClock;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

/**
 * author : Sy007
 * date   : 2020/11/29
 * version: 1.0
 */
public class BounceChecker {

    private Map<String, Long> temp = new HashMap<>();

    public boolean checkView(String owner, String methodName, View view, long checkTime) {
        String uniqueId = generateUniqueId(owner, methodName, view);
        return checkAny(uniqueId, checkTime);
    }

    public boolean checkAny(String uniqueId, long checkTime) {
        if (!temp.containsKey(uniqueId)) {
            temp.put(uniqueId, SystemClock.elapsedRealtime());
            return false;
        }
        Long lastClickTime = temp.get(uniqueId);
        boolean isBounce = SystemClock.elapsedRealtime() - lastClickTime < checkTime;
        if (!isBounce) {
            temp.put(uniqueId, SystemClock.elapsedRealtime());
        }
        return isBounce;
    }


    private static String generateUniqueId(String owner, String methodName, View view) {
        return owner + "|" + methodName + "|" + view.getId();
    }
}
