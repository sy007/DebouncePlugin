package com.sunyuan.simple.lib;

import android.util.Log;
import android.view.View;

import com.sunyuan.debounce.lib.BounceChecker;
import com.sunyuan.debounce.lib.InterfaceMethodProxy;
import com.sunyuan.debounce.lib.MethodHookParam;


/**
 * @author sy007
 * @date 2023/01/17
 * @description
 */
public class ClickMethodProxy {

    private static final long CHECK_TIME = 5000;

    private final BounceChecker checker = new BounceChecker();

    @InterfaceMethodProxy(
            ownerType = View.OnClickListener.class,
            methodName = "onClick",
            parameterTypes = {View.class},
            returnType = void.class)
    public boolean onClickProxy(MethodHookParam param) {
        boolean isBounce = checker.checkView(param.owner, param.methodName, (View) param.args[0], CHECK_TIME);
        Log.d("Debounce", "onClickProxy=>" + param.owner + "|" + param.methodName + "|" + "isBounce:" + isBounce);
        return isBounce;
    }
}

