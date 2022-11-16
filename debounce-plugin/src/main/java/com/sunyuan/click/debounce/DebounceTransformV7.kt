package com.sunyuan.click.debounce

import ClickMethodVisitor
import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import com.sunyuan.click.debounce.entity.MethodEntity
import com.sunyuan.click.debounce.extension.DebounceExtension
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.objectweb.asm.ClassVisitor

/**
 * @author sy007
 * @date 2022/11/13
 * @description
 */
abstract class DebounceTransformV7 : AsmClassVisitorFactory<DebounceTransformV7.Parameters> {

    interface Parameters : InstrumentationParameters {
        @get:Input
        @get:Optional
        val debug: Property<Boolean>

        @get:Input
        @get:Optional
        val generateReport: Property<Boolean>

        @get:Input
        @get:Optional
        val checkTime: Property<Long>

        @get:Input
        @get:Optional
        val includeForMethodAnnotation: SetProperty<String>

        @get:Input
        @get:Optional
        val excludeForMethodAnnotation: SetProperty<String>

        @get:Input
        @get:Optional
        val excludes: SetProperty<String>

        @get:Input
        @get:Optional
        val includes: SetProperty<String>

        @get:Input
        @get:Optional
        val hookMethodEntities: MapProperty<String, MethodEntity>

        @get:Input
        @get:Optional
        val hookInterfaces: SetProperty<String>
    }

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        val parameters = parameters.get()
        return ClickMethodVisitor(nextClassVisitor,
            hookMethodEntities = parameters.hookMethodEntities.get(),
            includeMethodOfAnnotation = {
                parameters.includeForMethodAnnotation.get().contains(it.desc)
            },
            excludeMethodOfAnnotation = {
                parameters.excludeForMethodAnnotation.get().contains(it.desc)
            },
            collectImplTargetInterfaces = { _ ->
                classContext.currentClassData.interfaces.map {
                    it.replace(".", "/")
                }.intersect(parameters.hookInterfaces.get())
            })
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        val parameters = parameters.get()
        return DebounceExtension.matchClassPath(
            classData.className.replace(".", "/") + ".class",
            parameters.includes.get(),
            parameters.excludes.get()
        )
    }
}

