package com.sunyuan.click.debounce.utils;


import com.sunyuan.click.debounce.entity.LambdaMethodEntity;
import com.sunyuan.click.debounce.entity.MethodEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * author : Sy007
 * date   : 2020/11/29
 * desc   :
 * version: 1.0
 */
public class MethodUtil {

    /**
     * 收集需要插桩的方法信息
     * key:className
     * value:key为methodName+methodDes,value为方法信息，需要注意的是MethodEntity#interfaceName为null
     */
    public static final Map<String, Map<String, MethodEntity>> sCollectDefaultMethods = new HashMap<>();

    /**
     * 收集需要插桩的Lambda方法信息
     * key:className
     * value:key为Lambda方法所在的className,value为Lambda方法信息,需要注意的是LambdaMethodEntity#interfaceName为null
     */
    public static final Map<String, Map<String, LambdaMethodEntity>> sCollectLambdaMethods = new HashMap<>();

}
