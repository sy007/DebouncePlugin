@file:Suppress("DEPRECATION")

package com.sunyuan.click.debounce.utils

import com.android.build.api.transform.TransformInput
import java.io.File
import java.net.MalformedURLException
import java.net.URLClassLoader

/**
 * author : Six
 * date   : 2021/1/16 001621:47
 */
object ClassLoaderHelper {

    fun getClassLoader(
        inputs: Collection<TransformInput>,
        referencedInputs: Collection<TransformInput>,
        bootClasspath: List<File>
    ): URLClassLoader {
        val compileClasspath = listOf(inputs, referencedInputs).flatten().map {
            it.jarInputs + it.directoryInputs
        }.flatten().map {
            it.file
        }
        val bootClassLoader =
            URLClassLoader(bootClasspath.map { it.toURI().toURL() }.toTypedArray(), null)
        return URLClassLoader(
            compileClasspath.map { it.toURI().toURL() }.toTypedArray(),
            bootClassLoader
        )
    }
}