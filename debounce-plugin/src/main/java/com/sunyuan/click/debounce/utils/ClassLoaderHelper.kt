package com.sunyuan.click.debounce.utils

import com.android.build.api.transform.TransformInput
import com.android.build.gradle.AppExtension
import com.google.common.collect.ImmutableList
import com.google.common.collect.Iterables
import com.sunyuan.click.debounce.getAndroidJarPath
import org.gradle.api.Project
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader

/**
 * author : Six
 * date   : 2021/1/16 001621:47
 * desc   : 参考Hunter框架
 * https://github.com/Leaking/Hunter/blob/master/hunter-transform/src/main/java/com/quinn/hunter/transform/asm/ClassLoaderHelper.java
 * version: 1.0
 */
object ClassLoaderHelper {
    @Throws(MalformedURLException::class)
    fun getClassLoader(
        inputs: Collection<TransformInput>,
        referencedInputs: Collection<TransformInput>,
        androidJarPath: String
    ): URLClassLoader {
        val urls = ImmutableList.Builder<URL>()
        val file = File(androidJarPath)
        val androidJarURL = file.toURI().toURL()
        urls.add(androidJarURL)
        for (totalInputs in Iterables.concat(inputs, referencedInputs)) {
            for (directoryInput in totalInputs.directoryInputs) {
                if (directoryInput.file.isDirectory) {
                    urls.add(directoryInput.file.toURI().toURL())
                }
            }
            for (jarInput in totalInputs.jarInputs) {
                if (jarInput.file.isFile) {
                    urls.add(jarInput.file.toURI().toURL())
                }
            }
        }
        val allUrls = urls.build()
        val classLoaderUrls = allUrls.toTypedArray()
        return URLClassLoader(classLoaderUrls)
    }
}