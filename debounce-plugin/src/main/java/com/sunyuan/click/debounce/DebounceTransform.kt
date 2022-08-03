package com.sunyuan.click.debounce

import CollectNeedHookMethodInformationVisitor
import com.android.build.api.transform.*
import com.android.build.api.variant.VariantInfo
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


/**
 * author : Sy007
 * date   : 2020/11/28
 * desc   :
 * version: 1.0
 */
private const val TRANSFORM_NAME = "DebounceTransform"

open class DebounceTransform(private val project: Project) : Transform() {
    private lateinit var mContext: Context
    private lateinit var mDebounceExtension: DebounceExtension
    private val mSpecifiedInterfaceImplChecked = SpecifiedInterfaceImplChecked()


    init {
        project.afterEvaluate {
            val debounceExtension: DebounceExtension =
                project.extensions.findByName(EXTENSION_NAME) as DebounceExtension
            ConfigUtil.sDebug = debounceExtension.isDebug
            ConfigUtil.sDebounceCheckTime = debounceExtension.debounceCheckTime
            LogUtil.sLogger = project.logger
            debounceExtension.init()
            debounceExtension.printlnConfigInfo()
            mDebounceExtension = debounceExtension
        }
    }


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

    override fun transform(transformInvocation: TransformInvocation?) {
        super.transform(transformInvocation)
        transformInvocation ?: return
        mContext = transformInvocation.context
        val inputs = transformInvocation.inputs
        val outputProvider = transformInvocation.outputProvider
        val isIncremental = transformInvocation.isIncremental
        if (!isIncremental) {
            //如果不支持增量更新。
            outputProvider.deleteAll()
        }
        val urlClassLoader: URLClassLoader =
            ClassLoaderHelper.getClassLoader(inputs, transformInvocation.referencedInputs, project)
        mSpecifiedInterfaceImplChecked.setUrlClassLoader(urlClassLoader)
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
    }


    private fun transformSingleFile(inputFile: File, inputDir: File, outputDir: File) {
        if (ClassUtil.checkClassName(inputFile.name)) {
            val relativePath: String =
                inputFile.absolutePath.replace(inputDir.absolutePath + File.separator, "")
            var modifiedBytes: ByteArray? = null
            if (mDebounceExtension.match(relativePath)) {
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
        val modifiedJar: File =
            JarUtil.modifyJarFile(
                inputJar,
                mContext.temporaryDir
            ) { relativePath, sourceBytes ->
                //relativePath->com/xxx/yyy/zzz.class
                if (mDebounceExtension.match(relativePath)) {
                    return@modifyJarFile modifyClass(sourceBytes)
                } else {
                    return@modifyJarFile null
                }
            }
        modifiedJar.copyTo(outputJar, true)
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
        val needHookMethodInformationVisitor =
            CollectNeedHookMethodInformationVisitor(cv, mSpecifiedInterfaceImplChecked)
        val cr = ClassReader(srcClass)
        cr.accept(needHookMethodInformationVisitor, ClassReader.EXPAND_FRAMES)
        return cw.toByteArray()
    }
}