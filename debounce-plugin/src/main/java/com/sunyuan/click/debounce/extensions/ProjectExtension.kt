package com.sunyuan.click.debounce.utils

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.didiglobal.booster.kotlinx.file
import com.sunyuan.click.debounce.EXTENSION_NAME
import com.sunyuan.click.debounce.config.DebounceExtension
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
    get() = plugins.hasPlugin(AppPlugin::class.java)

val Project.isLibrary
    get() = plugins.hasPlugin("com.android.library")


inline fun <reified T : BaseExtension> Project.getAndroid(): T =
    extensions.getByName("android") as T

val Project.debounceEx: DebounceExtension
    get() = extensions.findByName(EXTENSION_NAME) as DebounceExtension


fun Project.variants(block: (variant: BaseVariant) -> Unit) {
    val android = getAndroid<BaseExtension>()
    when (android) {
        is AppExtension -> android.applicationVariants
        is LibraryExtension -> android.libraryVariants
        else -> emptyList<BaseVariant>()
    }.takeIf<Collection<BaseVariant>> {
        it.isNotEmpty()
    }?.forEach {
        block(it)
    }
}



