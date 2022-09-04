package com.sunyuan.click.debounce

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.sunyuan.click.debounce.extension.DebounceExtension
import com.sunyuan.click.debounce.utils.ConfigUtil
import com.sunyuan.click.debounce.utils.LogUtil
import org.gradle.api.Project
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.PathMatcher

/**
 * @author sy007
 * @date 2022/08/31
 * @description
 */

private const val DEBOUNCE_ENABLE = "debounceEnable"
private const val GLOB_SYNTAX = "glob:"

fun File.file(vararg path: String) = File(this, path.joinToString(File.separator))

fun File.touch(): File {
    if (!this.exists()) {
        this.parentFile?.mkdirs()
        this.createNewFile()
    }
    return this
}

fun Project.getReportFile(dirName: String, fileName: String): File {
    return project.buildDir.file("reports", "debounce-plugin", dirName, fileName)
}


val Project.enablePlugin: Boolean
    get() = if (project.hasProperty(DEBOUNCE_ENABLE)) {
        project.properties[DEBOUNCE_ENABLE].toString().toBoolean()
    } else {
        true
    }

val Project.isApp: Boolean
    get() = project.plugins.hasPlugin(AppPlugin::class.java)

val Project.appEx: AppExtension
    get() = project.extensions.getByType(
        AppExtension::class.java
    )

val Project.debounceEx: DebounceExtension
    get() = extensions.findByName(EXTENSION_NAME) as DebounceExtension


fun Project.getAndroidJarPath(): String {
    var sdkDirectory = appEx.sdkDirectory.absolutePath
    val compileSdkVersion = appEx.compileSdkVersion
    sdkDirectory = sdkDirectory + File.separator + "platforms" + File.separator
    return sdkDirectory + compileSdkVersion + File.separator + "android.jar"
}


fun Set<String>.toPathMatchers(): MutableSet<PathMatcher> {
    val paths = this
    val matchers = mutableSetOf<PathMatcher>()
    if (paths.isEmpty()) {
        return matchers
    }
    for (path in paths) {
        try {
            val fs = FileSystems.getDefault()
            val matcher = fs.getPathMatcher(GLOB_SYNTAX + path)
            matchers.add(matcher)
        } catch (e: IllegalArgumentException) {
            LogUtil.error(
                String.format(
                    "Ignoring relativePath '{%s}' glob pattern.Because something unusual happened here '{%s}'",
                    path,
                    e
                )
            )
        }
    }
    return matchers
}