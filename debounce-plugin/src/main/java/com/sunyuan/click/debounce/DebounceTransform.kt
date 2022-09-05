package com.sunyuan.click.debounce

import CollectNeedModifyOfMethodVisitor
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.sunyuan.click.debounce.extension.DebounceExtension
import com.sunyuan.click.debounce.utils.*
import com.sunyuan.click.debounce.visitor.ClickClassVisitor
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.io.IOException
import java.net.URLClassLoader
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
    private lateinit var context: Context
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
        context = transformInvocation.context
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
            inputs, transformInvocation.referencedInputs,
            project.getAndroidJarPath()
        )
        findInterfaceImplHelper.setUrlClassLoader(urlClassLoader)
        try {
            inputs.forEach { transformInput ->
                transformInput.jarInputs.forEach { jarInput ->
                    val inputJar = jarInput.file
                    val outputJar = getOutPutJar(jarInput, outputProvider)
                    if (isIncremental) {
                        when (jarInput.status) {
                            Status.ADDED, Status.CHANGED -> transformJar(inputJar, outputJar)
                            Status.REMOVED -> FileUtils.delete(outputJar)
                            else -> {//null or  Status.NOTCHANGED

                            }
                        }
                    } else {
                        transformJar(inputJar, outputJar)
                    }
                }
                transformInput.directoryInputs.forEach { di ->
                    val inputDir: File = di.file
                    val outputDir = outputProvider.getContentLocation(
                        di.name,
                        di.contentTypes,
                        di.scopes,
                        Format.DIRECTORY
                    )
                    if (isIncremental) {
                        for ((inputFile, value) in di.changedFiles) {
                            when (value) {
                                Status.ADDED, Status.CHANGED -> {
                                    transformSingleFile(inputFile, inputDir, outputDir)
                                }
                                Status.REMOVED -> {
                                    val outputFile = toOutputFile(outputDir, inputDir, inputFile)
                                    FileUtils.deleteIfExists(outputFile)
                                }
                                else -> {//null or  Status.NOTCHANGED

                                }
                            }
                        }
                    } else {
                        if (inputDir.isDirectory) {
                            inputDir.copyRecursively(outputDir, true)
                            inputDir.walk()
                                .maxDepth(Int.MAX_VALUE)
                                .filter {
                                    it.isFile
                                }.forEach { inputFile ->
                                    transformSingleFile(inputFile, inputDir, outputDir)
                                }

                        }
                    }
                }
            }
        } finally {
            worker.shutdown()
            worker.awaitTermination(1, TimeUnit.HOURS)
        }
        LogUtil.warn("--------------------------------------------------------")
        val costTime: Long = System.currentTimeMillis() - startTime
        LogUtil.warn(name + " cost " + costTime + "ms")
        LogUtil.warn("--------------------------------------------------------")
    }


    private fun transformSingleFile(inputFile: File, inputDir: File, outputDir: File) {
        if (!ClassUtil.checkClassName(inputFile.name)) {
            return
        }
        worker.submit {
            val classPath: String =
                inputFile.absolutePath.replace(inputDir.absolutePath + File.separator, "")
            var modifiedBytes: ByteArray? = null
            if (debounceEx.matchClassPath(classPath)) {
                modifiedBytes = modifyClass(inputFile.readBytes())
            }
            if (modifiedBytes != null) {
                val outputFile = toOutputFile(outputDir, inputDir, inputFile)
                outputFile.writeBytes(modifiedBytes)
            }
        }
    }

    private fun toOutputFile(outputDir: File, inputDir: File, inputFile: File): File {
        val filePath: String = inputFile.absolutePath
        return File(filePath.replace(inputDir.absolutePath, outputDir.absolutePath))
    }


    private fun transformJar(
        inputJar: File,
        outputJar: File
    ) {
        worker.submit {
            val modifiedJar: File =
                JarUtil.modifyJarFile(
                    inputJar,
                    context.temporaryDir
                ) { classPath, sourceBytes ->
                    //classPath->com/xxx/yyy/zzz.class
                    val className = classPath.split("/").lastOrNull() ?: return@modifyJarFile null
                    if (ClassUtil.checkClassName(className) && debounceEx.matchClassPath(classPath)) {
                        return@modifyJarFile modifyClass(sourceBytes)
                    } else {
                        return@modifyJarFile null
                    }
                }
            modifiedJar.copyTo(outputJar, true)
        }
    }

    private fun getOutPutJar(jarInput: JarInput, outputProvider: TransformOutputProvider): File {
        var destName = jarInput.file.name
        val hexName = DigestUtils.md5Hex(
            jarInput.file.absolutePath
        ).substring(0, 8)
        if (destName.endsWith(".jar")) {
            destName = destName.substring(0, destName.length - 4)
        }
        return outputProvider.getContentLocation(
            destName + "_" + hexName,
            jarInput.contentTypes,
            jarInput.scopes,
            Format.JAR
        )
    }


    @Throws(IOException::class)
    fun modifyClass(srcClass: ByteArray): ByteArray? {
        val cw = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val cv = ClickClassVisitor(cw)
        val needModifyOfMethodVisitor =
            CollectNeedModifyOfMethodVisitor(cv, debounceEx, findInterfaceImplHelper)
        val cr = ClassReader(srcClass)
        cr.accept(needModifyOfMethodVisitor, ClassReader.EXPAND_FRAMES)
        return cw.toByteArray()
    }

}