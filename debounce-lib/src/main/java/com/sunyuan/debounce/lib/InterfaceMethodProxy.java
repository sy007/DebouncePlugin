package com.sunyuan.debounce.lib;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author sy007
 * @date 2023/01/17
 * @description
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InterfaceMethodProxy {
    //事件所属的接口类型
    Class<?> ownerType();

    //事件所属的方法名
    String methodName();

    //事件所属的方法参数列表类型
    Class<?>[] parameterTypes();

    //事件方法的返回类型
    Class<?> returnType();
}
