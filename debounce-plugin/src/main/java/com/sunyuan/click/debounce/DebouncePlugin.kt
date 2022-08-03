package com.sunyuan.click.debounce

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.sunyuan.click.debounce.extension.DebounceExtension
import com.sunyuan.click.debounce.utils.ConfigUtil
import com.sunyuan.click.debounce.utils.LogUtil
import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * author : Sy007
 * date   : 2020/11/28
 * desc   : Plugin入口
 * version: 1.0
 */

internal const val EXTENSION_NAME = "debounce"
private const val DEBOUNCE_ENABLE = "debounceEnable"

class DebouncePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val hasAppPlugin = target.plugins.hasPlugin(AppPlugin::class.java)
        if (hasAppPlugin) {
            val appExtension: AppExtension = target.extensions.getByType(
                AppExtension::class.java
            )
            target.extensions.create(EXTENSION_NAME, DebounceExtension::class.java, target)
            val isEnable = if (target.hasProperty(DEBOUNCE_ENABLE)) {
                target.properties[DEBOUNCE_ENABLE].toString().toBoolean()
            } else {
                true
            }
            if (!isEnable) {
                target.logger.warn("debounce function is off!")
                return
            }
            target.logger.warn("debounce function is on!")
            appExtension.registerTransform(DebounceTransform(target))
        }
    }
}