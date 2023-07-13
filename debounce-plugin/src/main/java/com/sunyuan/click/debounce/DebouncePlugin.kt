@file:Suppress("DEPRECATION")

package com.sunyuan.click.debounce

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.gradle.internal.pipeline.TransformTask
import com.sunyuan.click.debounce.config.DebounceExtension
import com.sunyuan.click.debounce.task.ModifyClassesTask
import com.sunyuan.click.debounce.transform.DebounceTransform
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

        val debounceEx = project.extensions.create(
            EXTENSION_NAME,
            DebounceExtension::class.java,
            project.objects
        )

        if (!project.enablePlugin) {
            LogUtil.warn("debounce-plugin is off.")
            return
        }

        LogUtil.warn("debounce-plugin is on.")

        when {
            VersionUtil.V7_4 -> {
                val androidComponents =
                    project.extensions.getByType(AndroidComponentsExtension::class.java)
                androidComponents.onVariants { variant ->
                    val taskProvider = project.tasks.register(
                        "${variant.name}DebounceModifyClasses",
                        ModifyClassesTask::class.java
                    )
                    variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                        .use(taskProvider)
                        .toTransform(
                            ScopedArtifact.CLASSES,
                            ModifyClassesTask::allJars,
                            ModifyClassesTask::allDirectories,
                            ModifyClassesTask::output
                        )
                }
            }
            else -> {
                project.getAndroid<BaseExtension>()
                    .registerTransform(DebounceTransform(project, debounceEx))
            }
        }
        project.afterEvaluate {
            project.debounceEx.init()
            project.transformCompletedListener {
                if (!debounceEx.generateReport.get()) {
                    return@transformCompletedListener
                }
                dump(project, it.name)
            }
        }
    }


    private fun Project.transformCompletedListener(complete: (variant: BaseVariant) -> Unit) {
        project.variants { variant ->
            val task = when {
                VersionUtil.V7_4 -> {
                    project.tasks.withType(ModifyClassesTask::class.java).find {
                        it.name.contains(variant.name)
                    }
                }
                else -> {
                    val transform = findLastClassesTransform()
                    project.tasks.withType(TransformTask::class.java).find { transformTask ->
                        transformTask.name.endsWith(variant.name.capitalize()) && transformTask.transform == transform
                    }
                }
            }
            task?.doLast {
                complete(variant)
            }
        }
    }

    private fun Project.findLastClassesTransform(): Transform {
        return project.getAndroid<BaseExtension>().transforms.reversed().firstOrNull {
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
        HtmlReportUtil().dump(file)
    }
}






