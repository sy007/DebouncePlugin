package com.sunyuan.debounce.lib;

import java.util.Arrays;

/**
 * @author sy007
 * @date 2023/01/17
 * @description
 */
public class MethodHookParam {
    public String owner;
    public String methodName;
    public Object[] args;

    private MethodHookParam() {

    }

    public static MethodHookParam newInstance(String owner, String methodName, Object[] args) {
        MethodHookParam descriptor = new MethodHookParam();
        descriptor.owner = owner;
        descriptor.methodName = methodName;
        descriptor.args = args;
        return descriptor;
    }

    public String generateUniqueId() {
        return owner + "|" + methodName + "|" + Arrays.toString(args);
    }
}
