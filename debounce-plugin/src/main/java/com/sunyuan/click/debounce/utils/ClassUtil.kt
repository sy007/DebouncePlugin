package com.sunyuan.click.debounce.utils

object ClassUtil {
    @JvmStatic
    fun checkClassName(className: String): Boolean {
        return className.endsWith(".class") && className != "R.class" &&
                !className.startsWith("R$") && className != "BuildConfig.class"
    }
}