package com.sunyuan.click.debounce

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.gradle.internal.pipeline.TransformTask
import com.sunyuan.click.debounce.extension.DebounceExtension
import com.sunyuan.click.debounce.utils.ConfigUtil
import com.sunyuan.click.debounce.utils.HtmlReport
import com.sunyuan.click.debounce.utils.LogUtil
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.text.SimpleDateFormat
import java.util.*


/**
 * author : Sy007
 * date   : 2020/11/28
 * desc   : Plugin入口
 * version: 1.0
 */
internal const val EXTENSION_NAME = "debounce"

class DebouncePlugin : Plugin<Project> {
    private lateinit var debounceTransform: DebounceTransform

    override fun apply(project: Project) {
        if (project.isApp) {
            LogUtil.init(project.logger)
            project.extensions.create(EXTENSION_NAME, DebounceExtension::class.java, project)
            if (!project.enablePlugin) {
                LogUtil.warn("debounce-plugin is off.")
                return
            }
            LogUtil.warn("debounce-plugin is on.")
            debounceTransform = DebounceTransform(project)
            project.appEx.registerTransform(debounceTransform)
            project.afterEvaluate {
                debounceExConfig(project)
                if (!project.debounceEx.generateReport) {
                    return@afterEvaluate
                }
                classesTransformCompleteListener(project) {
                    dump(project, it.name)
                }
            }
        }
    }

    private fun debounceExConfig(project: Project) {
        val debounceEx = project.debounceEx.apply {
            ConfigUtil.sDebug = isDebug
            ConfigUtil.sCheckTime = checkTime
            init()
            printlnConfigInfo()
        }
        debounceTransform.debounceEx = debounceEx
    }

    private fun classesTransformCompleteListener(
        project: Project,
        complete: (variant: BaseVariant) -> Unit
    ) {
        val appEx = project.appEx
        appEx.applicationVariants.forEach { variant ->
            val transform = findLastClassesTransform(appEx)
            val variantName = variant.name.capitalize()
            project.tasks.withType(TransformTask::class.java).find { transformTask ->
                transformTask.name.endsWith(variantName) && transformTask.transform == transform
            }?.doLast {
                complete(variant)
            }
        }
    }

    private fun findLastClassesTransform(appExtension: AppExtension): Transform {
        return appExtension.transforms.reversed().firstOrNull {
            it.scopes.containsAll(TransformManager.SCOPE_FULL_PROJECT)
                    && it.inputTypes.contains(QualifiedContent.DefaultContentType.CLASSES)
        } ?: throw GradleException("No available transform")
    }

    private fun dump(project: Project, dirName: String) {
        val file = project.getReportFile(
            dirName, "modified-method-list.html"
        )
        HtmlReport().dump(file)

    }
}