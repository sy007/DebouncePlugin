package com.sunyuan.click.debounce

import ClickMethodVisitor
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.sunyuan.click.debounce.extension.DebounceExtension
import com.sunyuan.click.debounce.utils.*
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.concurrent.TimeUnit


/**
 * author : Sy007
 * date   : 2020/11/28
 * desc   :
 * version: 1.0
 */
private const val TRANSFORM_NAME = "DebounceTransform"

open class DebounceTransform(private val project: Project) : Transform() {
    private val findInterfaceImplHelper = FindInterfaceImplHelper()
    private lateinit var worker: Worker
    internal lateinit var debounceEx: DebounceExtension

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
        return TransformManager.SCOPE_FULL_PROJECT
    }

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        MethodUtil.sModifyOfMethods.clear()
        val inputs = transformInvocation.inputs
        val outputProvider = transformInvocation.outputProvider
        val isIncremental = transformInvocation.isIncremental
        if (!isIncremental) {
            //如果不支持增量更新。
            outputProvider.deleteAll()
        }
        val startTime = System.currentTimeMillis()
        worker = Worker()
        val urlClassLoader: URLClassLoader = ClassLoaderHelper.getClassLoader(
            inputs,
            transformInvocation.referencedInputs,
            project.appEx.bootClasspath
        )
        findInterfaceImplHelper.setUrlClassLoader(urlClassLoader)
        try {
            inputs.forEach { transformInput ->
                transformInput.jarInputs.forEach { inputJar ->
                    worker.submit {
                        transformJar(inputJar, outputProvider, isIncremental)
                    }
                }
                transformInput.directoryInputs.forEach { dirInput ->
                    worker.submit {
                        transformDirInput(dirInput, outputProvider, isIncremental)
                    }
                }
            }
        } finally {
            worker.shutdown()
            worker.awaitTermination(1, TimeUnit.HOURS)
            urlClassLoader.use {
                it.close()
            }
        }
        LogUtil.warn("--------------------------------------------------------")
        val costTime: Long = System.currentTimeMillis() - startTime
        LogUtil.warn(name + " cost " + costTime + "ms")
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
                            dirInput.id,
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
                            dirInput.id,
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
                dirInput.id,
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
            inputJar.id,
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
                    FileUtils.delete(outputJar)
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
            val clickClassVisitor = ClickMethodVisitor(debounceEx, findInterfaceImplHelper)
            cr.accept(clickClassVisitor, 0)
            clickClassVisitor.accept(cw)
            cw.toByteArray()
        } else {
            bytes
        }

    private val QualifiedContent.id: String
        get() = DigestUtils.md5Hex(file.absolutePath)
}