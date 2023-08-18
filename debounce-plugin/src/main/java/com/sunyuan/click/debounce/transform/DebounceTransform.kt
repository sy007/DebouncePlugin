@file:Suppress("DEPRECATION")

package com.sunyuan.click.debounce.transform

import ClickMethodVisitor
import com.android.build.api.transform.*
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import com.didiglobal.booster.kotlinx.NCPU
import com.sunyuan.click.debounce.config.DebounceExtension
import com.sunyuan.click.debounce.entity.ProxyClassEntity
import com.sunyuan.click.debounce.extensions.transform
import com.sunyuan.click.debounce.utils.*
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.objectweb.asm.*
import java.util.concurrent.Callable


/**
 * author : Sy007
 * date   : 2020/11/28
 * desc   :
 * version: 1.0
 */
private const val TRANSFORM_NAME = "DebounceTransform"

open class DebounceTransform(
    private val project: Project,
    private val debounceEx: DebounceExtension
) : Transform() {

    private lateinit var proxyClassEntity: ProxyClassEntity

    override fun getName(): String {
        return TRANSFORM_NAME
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun isIncremental(): Boolean {
        return true
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return when {
            project.isApp -> TransformManager.SCOPE_FULL_PROJECT
            project.isLibrary -> TransformManager.PROJECT_ONLY
            else -> TODO("Not an Android project")
        }
    }

    override fun getReferencedScopes(): MutableSet<in QualifiedContent.Scope> {
        return when {
            project.isApp -> TransformManager.SCOPE_FULL_PROJECT
            project.isLibrary -> TransformManager.PROJECT_ONLY
            else -> TODO("Not an Android project")
        }
    }

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)

        val executor = Executors.newFixedThreadPool(NCPU)
        val inputs = transformInvocation.inputs
        val outputProvider = transformInvocation.outputProvider
        val isIncremental = transformInvocation.isIncremental
        if (!isIncremental) {
            //如果不支持增量更新。
            outputProvider.deleteAll()
        }
        val startTime: Long = System.currentTimeMillis()

        val compileClasspath = listOf(inputs, transformInvocation.referencedInputs).flatten().map {
            it.jarInputs + it.directoryInputs
        }.flatten().map {
            it.file
        }
        val urlClassLoader: URLClassLoader = ClassLoaderUtil.getClassLoader(
            compileClasspath,
            project.getAndroid<BaseExtension>().bootClasspath
        )

        InterfaceFinderUtil.setUrlClassLoader(urlClassLoader)

        ProxyClassParserUtil.parse(
            urlClassLoader,
            debounceEx.proxyClassName.get()
        )?.let {
            proxyClassEntity = it
        } ?: return

        try {
            inputs.asSequence().map {
                it.jarInputs + it.directoryInputs
            }.flatten().map { input ->
                executor.submit(Callable {
                    when (input) {
                        is DirectoryInput -> transformDirInput(
                            input,
                            outputProvider,
                            isIncremental
                        )

                        is JarInput -> transformJar(input, outputProvider, isIncremental)
                    }
                })
            }.forEach {
                it.get()
            }
        } finally {
            executor.shutdown()
            executor.awaitTermination(1, TimeUnit.HOURS)
        }

        LogUtil.warn("--------------------------------------------------------")
        val costTime: Long = System.currentTimeMillis() - startTime
        LogUtil.warn("DebounceTransform" + " cost " + costTime + "ms")
        LogUtil.warn("--------------------------------------------------------")
    }

    private fun transformDirInput(
        dirInput: DirectoryInput,
        outputProvider: TransformOutputProvider,
        isIncremental: Boolean
    ) {
        val inputDir: File = dirInput.file
        if (isIncremental) {
            dirInput.changedFiles.forEach { (file, status) ->
                when (status) {
                    Status.REMOVED -> {
                        outputProvider.getContentLocation(
                            dirInput.file.absolutePath,
                            dirInput.contentTypes,
                            dirInput.scopes,
                            Format.DIRECTORY
                        ).parentFile.listFiles()?.asSequence()
                            ?.filter { it.isDirectory }
                            ?.map { File(it, dirInput.file.toURI().relativize(file.toURI()).path) }
                            ?.filter { it.exists() }
                            ?.forEach { it.delete() }
                        file.delete()
                    }

                    Status.ADDED, Status.CHANGED -> {
                        val outputDir = outputProvider.getContentLocation(
                            dirInput.file.absolutePath,
                            dirInput.contentTypes,
                            dirInput.scopes,
                            Format.DIRECTORY
                        )
                        val output = File(outputDir, inputDir.toURI().relativize(file.toURI()).path)
                        file.transform(
                            output,
                            inputDir
                        ) { canonicalName: String, byteArray: ByteArray ->
                            transform(canonicalName, byteArray)
                        }
                    }

                    else -> {

                    }
                }
                return
            }
        } else {
            val outputDir = outputProvider.getContentLocation(
                dirInput.file.absolutePath,
                dirInput.contentTypes,
                dirInput.scopes,
                Format.DIRECTORY
            )
            inputDir.transform(outputDir, inputDir) { canonicalName: String, byteArray: ByteArray ->
                transform(canonicalName, byteArray)
            }
        }
    }

    private fun transformJar(
        inputJar: JarInput,
        provider: TransformOutputProvider,
        isIncremental: Boolean
    ) {
        val outputJar = provider.getContentLocation(
            inputJar.file.absolutePath,
            inputJar.contentTypes,
            inputJar.scopes, Format.JAR
        )
        if (isIncremental) {
            when (inputJar.status) {
                Status.ADDED, Status.CHANGED -> {
                    inputJar.file.transform(
                        outputJar
                    ) { canonicalName: String, byteArray: ByteArray ->
                        transform(canonicalName, byteArray)
                    }
                }

                Status.REMOVED -> {
                    outputJar.delete()
                }

                else -> {

                }
            }
            return
        }
        inputJar.file.transform(outputJar) { canonicalName: String, byteArray: ByteArray ->
            transform(canonicalName, byteArray)
        }
    }


    private fun transform(canonicalName: String, bytes: ByteArray): ByteArray =
        if (PathMatcherUtil.matchClassPath(canonicalName)) {
            val cr = ClassReader(bytes)
            val cw = ClassWriter(ClassWriter.COMPUTE_MAXS)
            val clickClassVisitor = ClickMethodVisitor(cw,
                proxyClassEntity,
                excludeMethodOfAnnotation = {
                    debounceEx.excludeForMethodAnnotation.get().contains(it.desc)
                })
            cr.accept(clickClassVisitor, 0)
            cw.toByteArray()
        } else {
            bytes
        }
}