package com.sunyuan.click.debounce.entity;

import com.android.annotations.NonNull;

import java.io.Serializable;

/**
 * author : Sy007
 * date   : 2020/11/28
 * desc   :
 * version: 1.0
 */
public class MethodEntity implements Serializable {
    // 方法名
    private String methodName;
    // 方法描述
    private String methodDesc;
    // 方法所在的接口
    private String interfaceName;

    private String name;


    public MethodEntity(@NonNull String name) {
        this.name = name;
    }


    public MethodEntity(String methodName, String methodDesc, String interfaceName) {
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.interfaceName = interfaceName;
    }



    public String getMethodName() {
        return methodName;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    /*support Gradle DLS*/
    public void methodName(String methodName) {
        this.methodName = methodName;
    }

    public void methodDesc(String methodDesc) {
        this.methodDesc = methodDesc;
    }

    public void interfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }
}
