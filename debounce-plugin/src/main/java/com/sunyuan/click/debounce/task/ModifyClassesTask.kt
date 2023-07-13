package com.sunyuan.click.debounce.task

import ClickMethodVisitor
import com.android.build.gradle.BaseExtension
import com.didiglobal.booster.kotlinx.NCPU
import com.didiglobal.booster.kotlinx.redirect
import com.didiglobal.booster.kotlinx.search
import com.sunyuan.click.debounce.entity.ProxyClassEntity
import com.sunyuan.click.debounce.extensions.isJarSignatureRelatedFiles
import com.sunyuan.click.debounce.extensions.transform
import com.sunyuan.click.debounce.utils.*
import org.apache.commons.compress.archivers.jar.JarArchiveEntry
import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/**
 * @author sy007
 * @date 2023/07/13
 * @description
 */
abstract class ModifyClassesTask : DefaultTask() {

    @get:InputFiles
    abstract val allJars: ListProperty<RegularFile>

    @get:InputFiles
    abstract val allDirectories: ListProperty<Directory>

    @get:OutputFile
    abstract val output: RegularFileProperty


    @TaskAction
    fun taskAction() {

        val startTime: Long = System.currentTimeMillis()

        val compileClasspath = (allDirectories.get().asSequence().map {
            it.asFile
        } + allJars.get().asSequence().map {
            it.asFile
        }).toList()

        val classLoader = ClassLoaderUtil.getClassLoader(
            compileClasspath, project.getAndroid<BaseExtension>().bootClasspath
        )

        InterfaceFinderUtil.setUrlClassLoader(classLoader)

        val proxyClassEntity =
            ProxyClassParserUtil.parse(classLoader, project.debounceEx.proxyClassName.get())
                ?: return

        val entries = mutableSetOf<String>()
        val jarOutput = JarOutputStream(BufferedOutputStream(FileOutputStream(output.get().asFile)))
        allJars.get().forEach { file ->
            val jarFile = JarFile(file.asFile)
            jarFile.entries().asSequence().filterNot {
                isJarSignatureRelatedFiles(it.name)
            }.forEach { jarEntry ->
                if (!entries.contains(jarEntry.name)) {
                    jarOutput.putNextEntry(JarEntry(jarEntry.name))
                    val bytes = when (jarEntry.name.substringAfterLast('.', "")) {
                        "class" -> jarFile.getInputStream(jarEntry).use { src ->
                            try {
                                transform(proxyClassEntity, jarEntry.name, src.readBytes())
                            } catch (e: Throwable) {
                                LogUtil.error("Broken class: ${jarFile.name}!/${jarEntry.name}")
                                jarFile.getInputStream(jarEntry).readBytes()
                            }
                        }
                        else -> jarFile.getInputStream(jarEntry).readBytes()
                    }
                    jarOutput.write(bytes)
                    jarOutput.closeEntry()
                    entries.add(jarEntry.name)
                } else {
                    LogUtil.error("Duplicated jar entry: ${jarFile.name}!/${jarEntry.name}")
                }
            }
            jarFile.close()
        }
        allDirectories.get().forEach { directory ->
            directory.asFile.walk().forEach { file ->
                if (file.isFile) {
                    val canonicalName = directory.asFile.toURI().relativize(file.toURI()).path
                    val bytes = when (file.extension) {
                        "class" -> {
                            try {
                                transform(proxyClassEntity, canonicalName, file.readBytes())
                            } catch (e: Throwable) {
                                LogUtil.error("Broken class: $canonicalName")
                                file.readBytes()
                            }
                        }
                        else -> {
                            file.readBytes()
                        }
                    }
                    jarOutput.putNextEntry(JarEntry(canonicalName.replace(File.separatorChar, '/')))
                    jarOutput.write(bytes)
                    jarOutput.closeEntry()
                }
            }
        }

        jarOutput.close()

        LogUtil.warn("--------------------------------------------------------")
        val costTime: Long = System.currentTimeMillis() - startTime
        LogUtil.warn("DebounceTransform" + " cost " + costTime + "ms")
        LogUtil.warn("--------------------------------------------------------")
    }


    private fun transform(
        entity: ProxyClassEntity, canonicalName: String, bytes: ByteArray
    ): ByteArray = if (PathMatcherUtil.matchClassPath(canonicalName)) {
        val cr = ClassReader(bytes)
        val cw = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val clickClassVisitor =
            ClickMethodVisitor(Opcodes.ASM7, cw, entity, excludeMethodOfAnnotation = {
                project.debounceEx.excludeForMethodAnnotation.get().contains(it.desc)
            })
        cr.accept(clickClassVisitor, 0)
        cw.toByteArray()
    } else {
        bytes
    }
}