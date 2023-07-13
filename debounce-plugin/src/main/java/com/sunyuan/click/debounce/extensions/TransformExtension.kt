package com.sunyuan.click.debounce.extensions

import com.didiglobal.booster.kotlinx.NCPU
import com.didiglobal.booster.kotlinx.redirect
import com.didiglobal.booster.kotlinx.search
import com.didiglobal.booster.kotlinx.touch
import org.apache.commons.compress.archivers.jar.JarArchiveEntry
import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.parallel.InputStreamSupplier
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.*
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
/**
 * Transform this file or directory to the output by the specified transformer
 *
 * @param output The output location
 * @param transformer The byte data transformer
 * @description 参考:Booster
 */
fun File.transform(
    output: File,
    inputDir: File? = null,
    transformer: (canonicalName: String, byteArray: ByteArray) -> ByteArray
) {
    when {
        isDirectory -> this.toURI().let { base ->
            this.search().parallelStream().forEach {
                it.transform(File(output, base.relativize(it.toURI()).path), inputDir, transformer)
            }
        }
        isFile -> when (extension.toLowerCase()) {
            "jar" -> JarFile(this).use {
                it.transform(output, { zipEntry ->
                    JarArchiveEntry(zipEntry)
                }, transformer)
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
    entryFactory: (ZipEntry) -> ZipArchiveEntry,
    transformer: (canonicalName: String, sourceBytes: ByteArray) -> ByteArray
) {
    val entries = mutableSetOf<String>()
    val creator = ParallelScatterZipCreator(
        ThreadPoolExecutor(
            NCPU,
            NCPU,
            0L,
            TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            Executors.defaultThreadFactory()
        ) { runnable, _ ->
            runnable.run()
        }
    )
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
    ZipArchiveOutputStream(output).use {
        creator.writeTo(it)
    }
}

fun ZipFile.transform(
    output: File,
    entryFactory: (ZipEntry) -> ZipArchiveEntry,
    transformer: (canonicalName: String, byteArray: ByteArray) -> ByteArray
) = output.touch().outputStream().buffered().use {
    transform(it, entryFactory, transformer)
}

fun ZipInputStream.transform(
    output: OutputStream,
    entryFactory: (ZipEntry) -> ZipArchiveEntry,
    transformer: (canonicalName: String, byteArray: ByteArray) -> ByteArray
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

    ZipArchiveOutputStream(output).use {
        creator.writeTo(it)
    }
}

fun ZipInputStream.transform(
    output: File,
    entryFactory: (ZipEntry) -> ZipArchiveEntry,
    transformer: (canonicalName: String, byteArray: ByteArray) -> ByteArray
) = output.touch().outputStream().buffered().use {
    transform(it, entryFactory, transformer)
}

private val JAR_SIGNATURE_EXTENSIONS = setOf("SF", "RSA", "DSA", "EC")

fun isJarSignatureRelatedFiles(name: String): Boolean {
    return name.startsWith("META-INF/") && name.substringAfterLast('.') in JAR_SIGNATURE_EXTENSIONS
}
