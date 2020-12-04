package com.sunyuan.click.debounce.utils;

public class ClassUtil {

    public static boolean checkClassName(String className) {
        return className.endsWith(".class") && !className.equals("R.class") &&
                !className.startsWith("R$") && !className.equals("BuildConfig.class");
    }
}