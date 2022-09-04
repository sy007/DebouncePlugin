package com.sunyuan.click.debounce.utils

import com.sunyuan.click.debounce.utils.ClassUtil.checkClassName
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.compress.utils.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

object JarUtil {
    @Throws(IOException::class)
    fun modifyJarFile(
        jarFile: File,
        tempDir: File,
        jarTransform: (classPath: String, sourceBytes: ByteArray) -> ByteArray?
    ): File {
        //临时输出文件
        val hexName = DigestUtils.md5Hex(jarFile.absolutePath).substring(0, 8)
        val outPutJar = File(tempDir, hexName + jarFile.name)
        val jarOutputStream = JarOutputStream(FileOutputStream(outPutJar))
        //原文件
        val originJar = JarFile(jarFile)
        val enumeration = originJar.entries()
        while (enumeration.hasMoreElements()) {
            val jarEntry = enumeration.nextElement()
            val inputStream = originJar.getInputStream(jarEntry)
            val entryName = jarEntry.name
            val destEntry = JarEntry(entryName)
            jarOutputStream.putNextEntry(destEntry)
            val sourceClassBytes = IOUtils.toByteArray(inputStream)
            var modifiedClassBytes: ByteArray? = null
            if (checkClassName(entryName)) {
                modifiedClassBytes = jarTransform(entryName, sourceClassBytes)
            }
            if (modifiedClassBytes == null) {
                jarOutputStream.write(sourceClassBytes)
            } else {
                jarOutputStream.write(modifiedClassBytes)
            }
            jarOutputStream.closeEntry()
        }
        jarOutputStream.close()
        originJar.close()
        return outPutJar
    }
}