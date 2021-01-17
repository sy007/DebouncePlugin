package com.sunyuan.debounce.lib;

import android.annotation.SuppressLint;
import android.os.SystemClock;
import android.util.Log;


/**
 * author : Sy007
 * date   : 2020/11/29
 * desc   : 注入类
 * version: 1.0
 * <p>
 * {@link DebounceCheck#sCheckTime}
 * {@link DebounceCheck#sDebug}
 * <p>
 * 这两个字段变量的值在编译时根据build.gradle配置动态改变
 * <p>
 * {@link DebounceCheck#DEBOUNCE_CHECK_INSTANCE}
 * 这个字段用于静态Lambda方法插桩
 */
public class DebounceCheck {
    private static long sCheckTime = 1000L;
    private static boolean sDebug = true;

    /**
     * 这个实例对象给静态Lambda方法使用的。
     * 1.当Lambda方法体中没有没有使用到 this、super 或者外部实例的成员时；
     * Lambda的定义称为non-instance-capturing lambdas，即Lambda方法被定义为静态方法
     * 这里的{@link DebounceCheck#DEBOUNCE_CHECK_INSTANCE}就是为了处理这种情况。
     * <p>
     * 2.当Lambda方法中使用到this,super或者外部实例的成员时；
     * Lambda的定义称为instance-capturing lambdas，即Lambda方法被定义为实例方法
     * 使用实例对象的debounceCheck字段判断,即调用debounceCheck.isShake()判断。
     * <p>
     * debounceCheck在实例对象的<init>中被初始化。
     */
    public static final DebounceCheck DEBOUNCE_CHECK_INSTANCE = new DebounceCheck();


    private long lastClickTime;


    /**
     * 判断是否是快速点击
     */
    public boolean isShake() {
        boolean isShake = SystemClock.elapsedRealtime() - lastClickTime < sCheckTime;
        if (sDebug) {
            String suffix = "[checkTime:" + sCheckTime + ",isShake:" + isShake + "]";
            Log.d("DebounceCheck", generateTag(suffix));
        }
        if (isShake) {
            return true;
        } else {
            lastClickTime = SystemClock.elapsedRealtime();
            return false;
        }
    }

    @SuppressLint("DefaultLocale")
    private static String generateTag(String suffix) {
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[4];
        String callerClazzName = stackTraceElement.getClassName();
        callerClazzName = callerClazzName.substring(callerClazzName.lastIndexOf(".") + 1);
        String tag = "%s.%s(L:%d)";
        tag = String.format(tag, new Object[]{callerClazzName, stackTraceElement.getMethodName(), Integer.valueOf(stackTraceElement.getLineNumber())});
        tag = tag + ":" + suffix;
        return tag;
    }
}
