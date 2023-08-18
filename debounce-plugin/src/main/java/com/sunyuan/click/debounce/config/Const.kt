package com.sunyuan.click.debounce.config

import com.sunyuan.click.debounce.entity.MethodMapperEntity
import org.objectweb.asm.Opcodes
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * author : Sy007
 * date   : 2020/11/29
 * desc   :
 * version: 1.0
 */
object Const {
    const val ASM_API = Opcodes.ASM7
    const val sMethodHookParamDesc = "Lcom/sunyuan/debounce/lib/MethodHookParam;"
    const val sClickDeBounceDesc = "Lcom/sunyuan/debounce/lib/ClickDeBounce;"
    const val sIgnoreClickDeBounceDesc = "Lcom/sunyuan/debounce/lib/IgnoreClickDeBounce;"
    const val sInterfaceMethodProxyDesc = "Lcom/sunyuan/debounce/lib/InterfaceMethodProxy;"
    const val sAnnotationMethodProxyDesc = "Lcom/sunyuan/debounce/lib/AnnotationMethodProxy;"
    const val sViewDesc = "Landroid/view/View;"

    /**
     * 收集需要插桩的方法信息
     * key:className
     * value:key为methodName+methodDes,value为方法映射信息
     */
    val sModifyOfMethods: ConcurrentMap<String, ConcurrentMap<String, MethodMapperEntity>> =
        ConcurrentHashMap()
}