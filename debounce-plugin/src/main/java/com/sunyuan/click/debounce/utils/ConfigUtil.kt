package com.sunyuan.click.debounce.utils


/**
 * author : Sy007
 * date   : 2020/11/29
 * desc   :
 * version: 1.0
 */
object ConfigUtil {

    const val sOwnerClassPath = "com/sunyuan/debounce/lib/BounceChecker.class"
    const val LambdaBSMOwner = "java/lang/invoke/LambdaMetafactory"

    /**
     * 实例方法插入的字节码
     */
    const val sOwner = "com/sunyuan/debounce/lib/BounceChecker"
    const val sInitName = "<init>"
    const val sInitDesc = "()V"
    const val sFieldName = "$\$bounceChecker"
    const val sFieldDesc = "Lcom/sunyuan/debounce/lib/BounceChecker;"
    const val sMethodName = "check"
    const val sMethodDesc = "()Z"

    /**
     * 静态属性插入的字节码(静态方法中使用)
     */
    const val sStaticFieldName = "$\$sBounceChecker"

    /**
     * 这里修改的是BounceChecker#sDebug和BounceChecker#sCheckTime
     * 具体修改的值由build.gradle中配置指定
     */
    const val sCheckTimeNameWithDes = "sCheckTimeJ"
    const val sDebugNameWithDes = "sDebugZ"

    var sDebug = false
    var sCheckTime = 1000L
}