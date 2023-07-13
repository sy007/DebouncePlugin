@file:Suppress("DEPRECATION")

package com.sunyuan.click.debounce.utils

import java.io.File
import java.net.URLClassLoader

/**
 * author : Six
 * date   : 2021/1/16 001621:47
 */
object ClassLoaderUtil {

    fun getClassLoader(
        compileClasspath: List<File>,
        bootClasspath: List<File>
    ): URLClassLoader {
        val bootClassLoader =
            URLClassLoader(bootClasspath.map { it.toURI().toURL() }.toTypedArray(), null)
        return URLClassLoader(
            compileClasspath.map { it.toURI().toURL() }.toTypedArray(),
            bootClassLoader
        )
    }
}