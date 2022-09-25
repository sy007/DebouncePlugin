package com.sunyuan.click.debounce.utils

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.didiglobal.booster.kotlinx.file
import com.sunyuan.click.debounce.EXTENSION_NAME
import com.sunyuan.click.debounce.extension.DebounceExtension
import org.gradle.api.Project
import java.io.File

/**
 * @author sy007
 * @date 2022/08/31
 * @description
 */

private const val DEBOUNCE_ENABLE = "debounceEnable"

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
