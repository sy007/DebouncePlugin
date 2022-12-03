@file:Suppress("DEPRECATION")

package com.sunyuan.click.debounce

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.gradle.internal.pipeline.TransformTask
import com.sunyuan.click.debounce.extension.DebounceExtension
import com.sunyuan.click.debounce.utils.*
import org.gradle.api.GradleException
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
    override fun apply(project: Project) {
        project.extensions.findByName("android")
            ?: throw GradleException("$project is not an Android project")
        LogUtil.init(project.logger)
        val debounceEx =
            project.extensions.create(EXTENSION_NAME, DebounceExtension::class.java, project)
        if (!project.enablePlugin) {
            LogUtil.warn("debounce-plugin is off.")
            return
        }
        LogUtil.warn("debounce-plugin is on.")

        project.getAndroid<BaseExtension>()
            .registerTransform(DebounceTransform(project, debounceEx))

        project.afterEvaluate {
            debounceExConfig(project)
            classesTransformCompleteListener(project) {
                if (!debounceEx.generateReport) {
                    return@classesTransformCompleteListener
                }
                dump(project, it.name)
            }
        }
    }


    private fun debounceExConfig(project: Project) {
        project.debounceEx.apply {
            ConfigUtil.sDebug = this.isDebug
            ConfigUtil.sCheckTime = this.checkTime
            init()
        }
    }

    private fun classesTransformCompleteListener(
        project: Project,
        complete: (variant: BaseVariant) -> Unit
    ) {
        val android = project.getAndroid<BaseExtension>()
        when (android) {
            is AppExtension -> android.applicationVariants
            is LibraryExtension -> android.libraryVariants
            else -> emptyList<BaseVariant>()
        }.takeIf<Collection<BaseVariant>> {
            it.isNotEmpty()
        }?.forEach { variant ->
            val transform = findLastClassesTransform(project, android)
            project.tasks.withType(TransformTask::class.java).find { transformTask ->
                transformTask.name.endsWith(variant.name.capitalize()) && transformTask.transform == transform
            }?.doLast {
                complete(variant)
            }
        }
    }

    private fun findLastClassesTransform(
        project: Project,
        baseExtension: BaseExtension
    ): Transform {
        return baseExtension.transforms.reversed().firstOrNull {
            val scopeFullProject = mutableSetOf<QualifiedContent.Scope>()
            when {
                project.isApp -> {
                    TransformManager.SCOPE_FULL_PROJECT
                }
                project.isLibrary -> {
                    TransformManager.PROJECT_ONLY
                }
                else -> TODO("Not an Android project")
            }.forEach { type ->
                scopeFullProject.add(type as QualifiedContent.Scope)
            }
            it.scopes.containsAll(scopeFullProject) && it.inputTypes.contains(QualifiedContent.DefaultContentType.CLASSES)
        } ?: throw GradleException("No available transform")
    }

    private fun dump(project: Project, dirName: String) {
        val file = project.getReportFile(
            dirName, "modified-method-list.html"
        )
        HtmlReport().dump(file)
    }
}






