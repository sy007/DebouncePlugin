@file:Suppress("DEPRECATION")

package com.sunyuan.click.debounce

import ClickMethodVisitor
import com.android.build.api.transform.*
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import com.didiglobal.booster.kotlinx.NCPU
import com.sunyuan.click.debounce.entity.MethodEntity
import com.sunyuan.click.debounce.entity.ProxyClassEntity
import com.sunyuan.click.debounce.extension.DebounceExtension
import com.sunyuan.click.debounce.utils.*
import com.sunyuan.click.debounce.visitor.ProxyClassVisitor
import org.gradle.api.Project
import org.objectweb.asm.*
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


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
    private lateinit var hookInterfaces: MutableSet<String>
    private lateinit var hookMethodEntities: MutableSet<MethodEntity>

    private val findImplTargetInterfaceHelper = FindImplTargetInterfaceHelper()

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
        clear()

        val executor = Executors.newFixedThreadPool(NCPU)
        val inputs = transformInvocation.inputs
        val outputProvider = transformInvocation.outputProvider
        val isIncremental = transformInvocation.isIncremental
        if (!isIncremental) {
            //如果不支持增量更新。
            outputProvider.deleteAll()
        }
        val startTime: Long = System.currentTimeMillis()

        val urlClassLoader: URLClassLoader = ClassLoaderHelper.getClassLoader(
            inputs,
            transformInvocation.referencedInputs,
            project.getAndroid<BaseExtension>().bootClasspath
        )
        findImplTargetInterfaceHelper.setUrlClassLoader(urlClassLoader)
        urlClassLoader.getResourceAsStream("${debounceEx.proxyClassName.replace(".", "/")}.class")
            ?.use {
                it.readBytes()
            }?.run {
                ClassReader(this)
            }?.accept(ProxyClassVisitor(), 0)
        val proxyClassEntity = HookManager.sProxyClassEntity
        if (proxyClassEntity == null) {
            LogUtil.warn("proxyClassEntity is null,${debounceEx.proxyClassName} create or not?")
            return
        }

        this.proxyClassEntity = proxyClassEntity
        this.hookInterfaces = HookManager.sHookInterfaces
        this.hookMethodEntities = HookManager.sHookMethodEntities

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

    private fun clear() {
        HookManager.sProxyClassEntity = null
        HookManager.sModifyOfMethods.clear()
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
        if (debounceEx.matchClassPath(canonicalName)) {
            val cr = ClassReader(bytes)
            val cw = ClassWriter(ClassWriter.COMPUTE_MAXS)
            val clickClassVisitor = ClickMethodVisitor(
                proxyClassEntity,
                cw,
                hookMethodEntities = hookMethodEntities,
                excludeMethodOfAnnotation = {
                    debounceEx.excludeForMethodAnnotation.contains(it.desc)
                }, { name ->
                    val implTargetInterfaces = mutableSetOf<String>()
                    findImplTargetInterfaceHelper.find(
                        name,
                        hookInterfaces,
                        implTargetInterfaces
                    )
                    implTargetInterfaces
                })
            cr.accept(clickClassVisitor, 0)
            cw.toByteArray()
        } else {
            bytes
        }
}