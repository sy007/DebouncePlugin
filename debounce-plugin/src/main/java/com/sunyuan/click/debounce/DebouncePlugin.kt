package com.sunyuan.click.debounce

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.sunyuan.click.debounce.extension.DebounceExtension
import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * author : Sy007
 * date   : 2020/11/28
 * desc   : Plugin入口
 * version: 1.0
 */

internal const val EXTENSION_NAME = "debounce"

class DebouncePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val hasAppPlugin = target.plugins.hasPlugin(AppPlugin::class.java)
        if (hasAppPlugin) {
            val appExtension: AppExtension = target.extensions.getByType(
                    AppExtension::class.java
            )
            target.extensions.create(EXTENSION_NAME, DebounceExtension::class.java, target)
            appExtension.registerTransform(DebounceTransform(target))
        }
    }
}