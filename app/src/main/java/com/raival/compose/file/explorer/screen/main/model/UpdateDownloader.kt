package com.raival.compose.file.explorer.screen.main.model

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.App.Companion.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads release APKs in-app and hands them to the system installer.
 * Debug builds fetch the `app-debug.apk` asset, release builds the release APK,
 * so an install never changes the app variant (and its data).
 */
object UpdateDownloader {
    fun updatesDir(): File = File(globalClass.cacheDir, "updates").apply { mkdirs() }

    fun localApkFor(tagName: String, assetName: String): File =
        File(updatesDir(), "${tagName}_$assetName")

    suspend fun download(
        url: String,
        destFile: File,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            destFile.parentFile?.mkdirs()
            // Resume-safe: fresh download each time to keep signature verification simple
            if (destFile.exists()) destFile.delete()
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("User-Agent", "Prism-File-Explorer")
                instanceFollowRedirects = true
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext false
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: -1L
            connection.inputStream.use { input ->
                destFile.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var downloaded = 0L
                    while (isActive) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        downloaded += n
                        if (total > 0) onProgress(downloaded.toFloat() / total)
                    }
                    if (!isActive) {
                        runCatching { destFile.delete() }
                        return@withContext false
                    }
                }
            }
            true
        } catch (e: Exception) {
            logger.logError(e)
            runCatching { destFile.delete() }
            false
        } finally {
            connection?.disconnect()
        }
    }

    fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${globalClass.packageName}.provider",
            apkFile
        )
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
