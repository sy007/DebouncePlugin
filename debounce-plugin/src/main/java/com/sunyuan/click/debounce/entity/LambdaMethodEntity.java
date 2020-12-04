package com.sunyuan.click.debounce.entity;

/**
 * author : Sy007
 * date   : 2020/11/29
 * desc   :
 * version: 1.0
 */
public class LambdaMethodEntity extends MethodEntity {
    public int tag;

    public LambdaMethodEntity(String methodName, String methodDesc, String interfaceName, int tag) {
        super(methodName, methodDesc, interfaceName);
        this.tag = tag;
    }
}
