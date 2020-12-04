package com.sunyuan.click.debounce.utils;


import com.sunyuan.click.debounce.entity.LambdaMethodEntity;
import com.sunyuan.click.debounce.entity.MethodEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * author : Sy007
 * date   : 2020/11/29
 * desc   :
 * version: 1.0
 */
public class MethodUtil {
    public static final Map<String, MethodEntity> sNormalMethods = new HashMap<>();

    //key为 className,value为Lambda方法集合信息，需要注意的是LambdaMethodEntity#interfaceName 为null
    public static final Map<String, List<LambdaMethodEntity>> sLambdaMethods = new HashMap<>();

    static {
        sNormalMethods.put("onClick(Landroid/view/View;)V", new MethodEntity(
                "onClick",
                "(Landroid/view/View;)V",
                "android/view/View$OnClickListener"));
    }
}
