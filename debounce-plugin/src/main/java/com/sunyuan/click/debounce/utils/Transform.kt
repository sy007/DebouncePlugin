package com.sunyuan.click.debounce.utils

import com.didiglobal.booster.kotlinx.NCPU
import com.didiglobal.booster.kotlinx.redirect
import com.didiglobal.booster.kotlinx.search
import com.didiglobal.booster.kotlinx.touch
import org.apache.commons.compress.archivers.jar.JarArchiveEntry
import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.parallel.InputStreamSupplier
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * Transform this file or directory to the output by the specified transformer
 *
 * @param output The output location
 * @param transformer The byte data transformer
 * @description 参考:Booster
 */
fun File.transform(
    output: File,
    transformer: (canonicalName: String, ByteArray) -> ByteArray,
    inputDir: File? = null
) {
    when {
        isDirectory -> this.toURI().let { base ->
            this.search().parallelStream().forEach {
                it.transform(File(output, base.relativize(it.toURI()).path), transformer, inputDir)
            }
        }
        isFile -> when (extension.toLowerCase()) {
            "jar" -> JarFile(this).use {
                it.transform(output, ::JarArchiveEntry, transformer)
            }
            "class" -> this.inputStream().use { fs ->
                val canonicalName = inputDir?.toURI()?.relativize(this.toURI())?.path ?: ""
                transformer(canonicalName, fs.readBytes()).redirect(output)
            }
            else -> this.copyTo(output, true)
        }
        else -> throw IOException("Unexpected file: ${this.canonicalPath}")
    }
}


fun ZipFile.transform(
    output: OutputStream,
    entryFactory: (ZipEntry) -> ZipArchiveEntry = ::ZipArchiveEntry,
    transformer: (canonicalName: String, sourceBytes: ByteArray) -> ByteArray
) {
    val entries = mutableSetOf<String>()
    val creator = ParallelScatterZipCreator(ThreadPoolExecutor(
        NCPU, NCPU,
        0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(), object : ThreadFactory {
            private var threadNumber = AtomicInteger(1)
            override fun newThread(r: Runnable): Thread {
                return Thread(r).apply {
                    name = "debounce-transform-jar-thread-" + threadNumber.getAndIncrement()
                    isDaemon = true
                    priority = Thread.NORM_PRIORITY
                }
            }
        }) { r, _ -> r.run() })

    entries().asSequence().filterNot {
        isJarSignatureRelatedFiles(it.name)
    }.forEach { entry ->
        if (!entries.contains(entry.name)) {
            val zae = entryFactory(entry)
            val stream = InputStreamSupplier {
                when (entry.name.substringAfterLast('.', "")) {
                    "class" -> getInputStream(entry).use { src ->
                        try {
                            transformer(entry.name, src.readBytes()).inputStream()
                        } catch (e: Throwable) {
                            System.err.println("Broken class: ${this.name}!/${entry.name}")
                            getInputStream(entry)
                        }
                    }
                    else -> getInputStream(entry)
                }
            }
            creator.addArchiveEntry(zae, stream)
            entries.add(entry.name)
        } else {
            System.err.println("Duplicated jar entry: ${this.name}!/${entry.name}")
        }
    }
    ZipArchiveOutputStream(output).use(creator::writeTo)
}

fun ZipFile.transform(
    output: File,
    entryFactory: (ZipEntry) -> ZipArchiveEntry = ::ZipArchiveEntry,
    transformer: (canonicalName: String, ByteArray) -> ByteArray
) = output.touch().outputStream().buffered().use {
    transform(it, entryFactory, transformer)
}

fun ZipInputStream.transform(
    output: OutputStream,
    entryFactory: (ZipEntry) -> ZipArchiveEntry = ::ZipArchiveEntry,
    transformer: (canonicalName: String, ByteArray) -> ByteArray
) {
    val creator = ParallelScatterZipCreator()
    val entries = mutableSetOf<String>()

    while (true) {
        val entry = nextEntry?.takeUnless {
            isJarSignatureRelatedFiles(it.name)
        } ?: break
        if (!entries.contains(entry.name)) {
            val zae = entryFactory(entry)
            val data = readBytes()
            val stream = InputStreamSupplier {
                transformer(entry.name, data).inputStream()
            }
            creator.addArchiveEntry(zae, stream)
            entries.add(entry.name)
        }
    }

    ZipArchiveOutputStream(output).use(creator::writeTo)
}

fun ZipInputStream.transform(
    output: File,
    entryFactory: (ZipEntry) -> ZipArchiveEntry = ::ZipArchiveEntry,
    transformer: (canonicalName: String, ByteArray) -> ByteArray
) = output.touch().outputStream().buffered().use {
    transform(it, entryFactory, transformer)
}

private val JAR_SIGNATURE_EXTENSIONS = setOf("SF", "RSA", "DSA", "EC")

private fun isJarSignatureRelatedFiles(name: String): Boolean {
    return name.startsWith("META-INF/") && name.substringAfterLast('.') in JAR_SIGNATURE_EXTENSIONS
}

private const val DEFAULT_BUFFER_SIZE = 8 * 1024

private fun InputStream.readBytes(estimatedSize: Int = DEFAULT_BUFFER_SIZE): ByteArray {
    val buffer = ByteArrayOutputStream(maxOf(estimatedSize, this.available()))
    copyTo(buffer)
    return buffer.toByteArray()
}

private fun InputStream.copyTo(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    var bytesCopied: Long = 0
    val buffer = ByteArray(maxOf(bufferSize, this.available()))
    var bytes = read(buffer)
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        bytes = read(buffer)
    }
    return bytesCopied
}
