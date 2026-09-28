package com.raival.compose.file.explorer.screen.main.tab.files.zip

import com.raival.compose.file.explorer.App.Companion.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class ArchiveEntryInfo(
    val name: String,
    val isDirectory: Boolean,
    val size: Long
)

object ArchiveHelper {
    val supportedBrowsable = setOf("7z", "tar", "gz", "tgz", "tbz2", "bz2", "xz")

    fun isSupportedArchiveExt(ext: String): Boolean =
        ext.lowercase() in supportedBrowsable

    suspend fun listEntries(file: File): List<ArchiveEntryInfo> =
        withContext(Dispatchers.IO) {
            try {
                when (file.extension.lowercase()) {
                    "7z" -> listSevenZ(file)
                    "tar" -> listTar(FileInputStream(file))
                    "gz", "tgz" -> listTar(GzipCompressorInputStream(BufferedInputStream(FileInputStream(file))))
                    "bz2", "tbz2" -> listTar(BZip2CompressorInputStream(BufferedInputStream(FileInputStream(file))))
                    "xz" -> listTar(XZCompressorInputStream(BufferedInputStream(FileInputStream(file))))
                    else -> emptyList()
                }
            } catch (e: Exception) {
                logger.logError(e)
                emptyList()
            }
        }

    private fun listSevenZ(file: File): List<ArchiveEntryInfo> {
        val out = arrayListOf<ArchiveEntryInfo>()
        try {
            SevenZFile(file).use { sevenZ ->
                var entry = sevenZ.nextEntry
                while (entry != null) {
                    out.add(ArchiveEntryInfo(entry.name, entry.isDirectory, entry.size))
                    entry = sevenZ.nextEntry
                }
            }
        } catch (e: Exception) {
            logger.logError(e)
        }
        return out
    }

    private fun listTar(input: java.io.InputStream): List<ArchiveEntryInfo> {
        val out = arrayListOf<ArchiveEntryInfo>()
        try {
            TarArchiveInputStream(input).use { tar ->
                var entry = tar.nextEntry
                while (entry != null) {
                    out.add(ArchiveEntryInfo(entry.name, entry.isDirectory, entry.size))
                    entry = tar.nextEntry
                }
            }
        } catch (e: Exception) {
            logger.logError(e)
        } finally {
            runCatching { input.close() }
        }
        return out
    }

    suspend fun extractTo(file: File, destDir: File, password: CharArray? = null): Boolean =
        withContext(Dispatchers.IO) {
            try {
                destDir.mkdirs()
                when (file.extension.lowercase()) {
                    "7z" -> extractSevenZ(file, destDir, password)
                    "tar" -> extractTar(FileInputStream(file), destDir)
                    "gz", "tgz" -> extractTar(
                        GzipCompressorInputStream(BufferedInputStream(FileInputStream(file))),
                        destDir
                    )
                    "bz2", "tbz2" -> extractTar(
                        BZip2CompressorInputStream(BufferedInputStream(FileInputStream(file))),
                        destDir
                    )
                    "xz" -> extractTar(
                        XZCompressorInputStream(BufferedInputStream(FileInputStream(file))),
                        destDir
                    )
                    else -> return@withContext false
                }
                true
            } catch (e: Exception) {
                logger.logError(e)
                false
            }
        }

    private fun extractSevenZ(file: File, destDir: File, password: CharArray?): Boolean {
        return try {
            (if (password != null) SevenZFile(file, password) else SevenZFile(file)).use { sevenZ ->
                var entry = sevenZ.nextEntry
                val buf = ByteArray(8192)
                while (entry != null) {
                    val outFile = File(destDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            var n: Int
                            while (sevenZ.read(buf).also { n = it } > 0) fos.write(buf, 0, n)
                        }
                    }
                    entry = sevenZ.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            logger.logError(e)
            false
        }
    }

    private fun extractTar(input: java.io.InputStream, destDir: File) {
        TarArchiveInputStream(input).use { tar ->
            var entry = tar.nextEntry
            val buf = ByteArray(8192)
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                // Zip-slip guard
                if (!outFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                    entry = tar.nextEntry
                    continue
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        var n: Int
                        while (tar.read(buf).also { n = it } != -1) fos.write(buf, 0, n)
                    }
                }
                entry = tar.nextEntry
            }
        }
        runCatching { input.close() }
    }
}
