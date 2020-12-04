package com.sunyuan.click.debounce.utils;

/**
 * author : Sy007
 * date   : 2020/11/29
 * desc   :
 * version: 1.0
 */
public class ConfigUtil {
    //其他实例方法插入的字节码
    public static final String sOwner = "com/sunyuan/debounce/lib/DebounceCheck";
    public static final String sInitName = "<init>";
    public static final String sInitDesc = "()V";
    public static final String sFieldName = "$$debounceCheck";
    public static final String sFieldDesc = "Lcom/sunyuan/debounce/lib/DebounceCheck;";
    public static final String sMethodName = "isShake";
    public static final String sMethodDesc = "()Z";

    //静态Lambda方法插入的字节码
    public static final String sStaticLambdaOwner = "com/sunyuan/debounce/lib/DebounceCheck";
    public static final String sStaticLambdaName = "DEBOUNCE_CHECK_INSTANCE";
    public static final String sStaticLambdaDesc = "Lcom/sunyuan/debounce/lib/DebounceCheck;";

    //用于在ASM访问类信息时判断是否需要修改类中指定变量的值
    //这里修改的是DebounceCheck#IS_DEBUG和DebounceCheckCHECK_TIME
    //具体修改的值由build.gradle中配置指定
    public static final String sIsDebugFieldName = "sDebug";
    public static final String sDebounceCheckTimeFieldName = "sCheckTime";

    //根据build.gradle中配置的参数，动态修改DebounceCheck中IS_DEBUG和CHECK_TIME的值
    public static Boolean sDebug = false;
    public static Long sDebounceCheckTime = 1000L;
}
