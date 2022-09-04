package com.sunyuan.click.debounce.utils

import com.sunyuan.click.debounce.entity.MethodEntity
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * author : Sy007
 * date   : 2020/11/29
 * desc   :
 * version: 1.0
 */
object MethodUtil {
    /**
     * 收集需要插桩的方法信息
     * key:className
     * value:key为methodName+methodDes,value为方法信息，需要注意的是MethodEntity#interfaceName为null
     */
    val sModifyOfMethods: ConcurrentMap<String, ConcurrentMap<String, MethodEntity>> = ConcurrentHashMap()
}