@file:Suppress("DEPRECATION")

package com.sunyuan.click.debounce

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.tasks.TransformClassesWithAsmTask
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
        if (project.isApp) {
            LogUtil.init(project.logger)
            val debounceEx = project.extensions.create(EXTENSION_NAME, DebounceExtension::class.java, project)
            if (!project.enablePlugin) {
                LogUtil.warn("debounce-plugin is off.")
                return
            }
            LogUtil.warn("debounce-plugin is on.")
            when {
                Version.V7_X -> {
                    val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
                    androidComponents.onVariants { variant ->
                        variant.instrumentation.transformClassesWith(
                            DebounceTransformV7::class.java,
                            InstrumentationScope.ALL) {
                            it.debug.set(debounceEx.isDebug)
                            it.generateReport.set(debounceEx.generateReport)
                            it.checkTime.set(debounceEx.checkTime)
                            it.includes.set(debounceEx.includes)
                            it.excludes.set(debounceEx.excludes)
                            it.includeForMethodAnnotation.set(debounceEx.includeForMethodAnnotation)
                            it.excludeForMethodAnnotation.set(debounceEx.excludeForMethodAnnotation)
                            it.hookMethodEntities.set(debounceEx.hookMethodEntities)
                            it.hookInterfaces.set(debounceEx.hookInterfaces)
                        }
                        variant.instrumentation.setAsmFramesComputationMode(
                            FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS
                        )
                    }
                }
                else -> {
                    project.appEx.registerTransform(DebounceTransform(project, debounceEx))
                }
            }
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
        val appEx = project.appEx
        appEx.applicationVariants.forEach { variant ->
            val task = when {
                Version.V7_X -> {
                    val variantName = variant.name.capitalize()
                    project.tasks.withType(TransformClassesWithAsmTask::class.java).find {
                        it.name.contains(variantName)
                    }
                }
                else -> {
                    val transform = findLastClassesTransform(appEx)
                    val variantName = variant.name.capitalize()
                    project.tasks.withType(TransformTask::class.java).find { transformTask ->
                        transformTask.name.endsWith(variantName) && transformTask.transform == transform
                    }
                }
            }
            var startTime: Long = System.currentTimeMillis()
            task?.doFirst {
                startTime = System.currentTimeMillis()
                MethodUtil.sModifyOfMethods.clear()
            }?.doLast {
                project.debounceEx.clear()
                LogUtil.warn("--------------------------------------------------------")
                val costTime: Long = System.currentTimeMillis() - startTime
                LogUtil.warn("DebounceTransform" + " cost " + costTime + "ms")
                LogUtil.warn("--------------------------------------------------------")
                complete(variant)
            }
        }
    }

    private fun findLastClassesTransform(appExtension: AppExtension): Transform {
        return appExtension.transforms.reversed().firstOrNull {
            val scopeFullProject = mutableSetOf<QualifiedContent.Scope>()
            TransformManager.SCOPE_FULL_PROJECT.forEach { type ->
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






