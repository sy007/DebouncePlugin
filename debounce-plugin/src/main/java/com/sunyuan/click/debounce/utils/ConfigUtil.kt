package com.sunyuan.click.debounce.utils

import com.sunyuan.click.debounce.entity.MethodEntity
import java.util.*

/**
 * author : Sy007
 * date   : 2020/11/29
 * desc   :
 * version: 1.0
 */
object ConfigUtil {

    /**
     * 配置插桩的方法信息
     */
    val sHookMethods: MutableMap<String, MethodEntity> = HashMap()

    /**
     * 用于判断当前扫描的类是否是插桩接口的直接或间接子类
     */
    val sInterfaceSet: MutableSet<String> = mutableSetOf()

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
    const val sDebugFieldName = "sDebug"
    const val sBounceCheckTimeFieldName = "sCheckTime"

    var sDebug = false
    var sCheckTime = 1000L

    init {
        sHookMethods["onClick(Landroid/view/View;)V"] = MethodEntity().apply {
            methodName = "onClick"
            methodDesc = "(Landroid/view/View;)V"
            interfaceName = "android/view/View\$OnClickListener"
        }
    }
}