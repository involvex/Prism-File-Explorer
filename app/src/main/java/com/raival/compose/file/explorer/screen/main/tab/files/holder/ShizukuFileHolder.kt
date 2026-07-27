package com.raival.compose.file.explorer.screen.main.tab.files.holder

import android.content.Intent
import android.os.ParcelFileDescriptor
import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.R
import com.raival.compose.file.explorer.common.emptyString
import com.raival.compose.file.explorer.common.toFormattedSize
import com.raival.compose.file.explorer.screen.main.tab.files.misc.ContentCount
import com.raival.compose.file.explorer.screen.main.tab.files.misc.FileMimeType
import com.raival.compose.file.explorer.screen.main.tab.files.misc.FileMimeType.anyFileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShizukuFileHolder(val path: String) : ContentHolder() {
    private val shizukuManager get() = globalClass.shizukuManager

    override val uniquePath: String = path

    override val displayName: String by lazy {
        path.substringAfterLast('/').ifEmpty { path }
    }

    override val isFolder: Boolean by lazy { path.endsWith("/") || !displayName.contains(".") }

    override val lastModified: Long by lazy {
        try {
            shizukuManager.runShellCommand("stat -c %Y '$path' 2>/dev/null")?.trim()?.toLongOrNull() ?: 0L
        } catch (_: Exception) { 0L }
    }

    override val size: Long by lazy {
        try {
            if (isFolder) 0L
            else shizukuManager.runShellCommand("stat -c %s '$path' 2>/dev/null")?.trim()?.toLongOrNull() ?: 0L
        } catch (_: Exception) { 0L }
    }

    override val extension: String by lazy {
        if (isFolder) emptyString
        else displayName.substringAfterLast('.', emptyString).lowercase()
    }

    override val canAddNewContent: Boolean = true

    override val canRead: Boolean by lazy { shizukuManager.isAccessible() }

    override val canWrite: Boolean by lazy { shizukuManager.isAccessible() }

    override suspend fun listContent(): ArrayList<ShizukuFileHolder> {
        return withContext(Dispatchers.IO) {
            if (!shizukuManager.isAccessible()) return@withContext arrayListOf()
            val result = arrayListOf<ShizukuFileHolder>()
            try {
                val output = shizukuManager.runShellCommand("ls -1 '$path' 2>/dev/null") ?: return@withContext result
                output.lines().filter { it.isNotBlank() }.forEach { name ->
                    val childPath = if (path.endsWith("/")) "$path$name" else "$path/$name"
                    result.add(ShizukuFileHolder(childPath))
                }
            } catch (_: Exception) { }
            result
        }
    }

    override suspend fun getParent(): ShizukuFileHolder? {
        if (path == "/") return null
        val parentPath = path.trimEnd('/').substringBeforeLast('/').ifEmpty { "/" }
        return ShizukuFileHolder(if (parentPath == "/") "/" else "$parentPath/")
    }

    override suspend fun getContentCount(): ContentCount {
        if (!shizukuManager.isAccessible()) return ContentCount(0, 0)
        var folders = 0
        var files = 0
        try {
            val output = shizukuManager.runShellCommand("ls -1F '$path' 2>/dev/null") ?: return ContentCount(0, 0)
            output.lines().filter { it.isNotBlank() }.forEach { name ->
                if (name.endsWith("/")) folders++ else files++
            }
        } catch (_: Exception) { }
        return ContentCount(files, folders)
    }

    override suspend fun findFile(name: String): ShizukuFileHolder? {
        if (!shizukuManager.isAccessible()) return null
        val childPath = if (path.endsWith("/")) "$path$name" else "$path/$name"
        val exists = shizukuManager.runShellCommand("test -e '$childPath' && echo 'yes'")?.trim() == "yes"
        return if (exists) ShizukuFileHolder(childPath) else null
    }

    override suspend fun isValid(): Boolean =
        shizukuManager.isAccessible() &&
            (shizukuManager.runShellCommand("test -e '$path' && echo 'yes'")?.trim() == "yes")

    override suspend fun getDetails(): String {
        val separator = " | "
        return buildString {
            append(getLastModifiedDate())
            if (isFolder) {
                if (shizukuManager.isAccessible()) {
                    append(separator)
                    append(getFormattedFileCount())
                }
            } else {
                append(separator)
                append(size.toFormattedSize())
                append(separator)
                append(extension)
            }
        }
    }

    override fun open(
        context: android.content.Context,
        anonymous: Boolean,
        skipSupportedExtensions: Boolean,
        customMimeType: String?
    ) {
        if (!shizukuManager.isAccessible()) {
            globalClass.showMsg(R.string.cant_access_content)
            return
        }
        val uri = shizukuManager.getUriForFile(java.io.File(path))
            ?: run {
                globalClass.showMsg(R.string.failed_to_open_this_file)
                return
            }
        val mimeType = customMimeType
            ?: extension.toMimeType()
            ?: anyFileType
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                    or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        try {
            context.startActivity(intent)
        } catch (_: android.content.ActivityNotFoundException) {
            if (!anonymous) {
                open(context, anonymous = true, skipSupportedExtensions = true, null)
            } else {
                globalClass.showMsg(R.string.no_app_can_open_file)
            }
        } catch (e: Exception) {
            globalClass.logger.logError(e)
            globalClass.showMsg(R.string.failed_to_open_this_file)
        }
    }

    override suspend fun createSubFile(name: String, onCreated: (ContentHolder?) -> Unit) {
        if (!shizukuManager.isAccessible()) {
            onCreated(null)
            return
        }
        val newPath = if (path.endsWith("/")) "$path$name" else "$path/$name"
        val result = shizukuManager.runShellCommand("touch '$newPath' 2>&1")
        if (result != null && !result.contains("error") && !result.contains("denied")) {
            onCreated(ShizukuFileHolder(newPath))
        } else {
            onCreated(null)
        }
    }

    override suspend fun createSubFolder(name: String, onCreated: (ContentHolder?) -> Unit) {
        if (!shizukuManager.isAccessible()) {
            onCreated(null)
            return
        }
        val newPath = if (path.endsWith("/")) "$path$name" else "$path/$name"
        val result = shizukuManager.runShellCommand("mkdir -p '$newPath' 2>&1")
        if (result != null && !result.contains("error") && !result.contains("denied")) {
            onCreated(ShizukuFileHolder(newPath))
        } else {
            onCreated(null)
        }
    }

    suspend fun deleteShizukuFile(): Boolean {
        if (!shizukuManager.isAccessible()) return false
        return withContext(Dispatchers.IO) {
            try {
                val cmd = if (isFolder) "rm -rf '$path'" else "rm -f '$path'"
                val result = shizukuManager.runShellCommand("$cmd 2>&1")
                result == null || !result.contains("denied")
            } catch (_: Exception) { false }
        }
    }

    suspend fun copyToShizuku(dest: ShizukuFileHolder): Boolean {
        if (!shizukuManager.isAccessible()) return false
        return withContext(Dispatchers.IO) {
            try {
                val destPath = if (dest.path.endsWith("/")) {
                    "${dest.path}$displayName"
                } else {
                    "${dest.path}/$displayName"
                }
                val cmd = if (isFolder) "cp -r '$path' '$destPath'" else "cp '$path' '$destPath'"
                val result = shizukuManager.runShellCommand("$cmd 2>&1")
                result == null || !result.contains("denied")
            } catch (_: Exception) { false }
        }
    }

    suspend fun moveToShizuku(dest: ShizukuFileHolder): Boolean {
        if (!shizukuManager.isAccessible()) return false
        return withContext(Dispatchers.IO) {
            try {
                val destPath = if (dest.path.endsWith("/")) {
                    "${dest.path}$displayName"
                } else {
                    "${dest.path}/$displayName"
                }
                val result = shizukuManager.runShellCommand("mv '$path' '$destPath' 2>&1")
                result == null || !result.contains("denied")
            } catch (_: Exception) { false }
        }
    }

    suspend fun renameShizukuFile(newName: String): Boolean {
        if (!shizukuManager.isAccessible()) return false
        return withContext(Dispatchers.IO) {
            try {
                val parentPath = path.trimEnd('/').substringBeforeLast('/')
                val newPath = "$parentPath/$newName"
                val result = shizukuManager.runShellCommand("mv '$path' '$newPath' 2>&1")
                result == null || !result.contains("denied")
            } catch (_: Exception) { false }
        }
    }

    fun getSourceInputStream(): ParcelFileDescriptor? {
        if (!shizukuManager.isAccessible()) return null
        return try {
            val uri = shizukuManager.getUriForFile(java.io.File(path)) ?: return null
            globalClass.contentResolver.openFileDescriptor(uri, "r")
        } catch (_: Exception) { null }
    }

    fun getMimeType(): String? = extension.toMimeType()

    private fun String.toMimeType(): String? {
        return when (this.lowercase()) {
            in FileMimeType.imageFileType -> "image/*"
            in FileMimeType.videoFileType -> "video/*"
            in FileMimeType.audioFileType -> "audio/*"
            in FileMimeType.pdfFileType -> "application/pdf"
            in FileMimeType.codeFileType, in FileMimeType.editableFileType -> "text/*"
            in FileMimeType.supportedArchiveFileType -> {
                when (this.lowercase()) {
                    "zip" -> "application/zip"
                    "rar" -> "application/x-rar-compressed"
                    "7z" -> "application/x-7z-compressed"
                    "tar" -> "application/x-tar"
                    "gz" -> "application/gzip"
                    else -> null
                }
            }
            "apk" -> "application/vnd.android.package-archive"
            else -> null
        }
    }

    fun runShellCommand(command: String): String? {
        return shizukuManager.runShellCommand(command)
    }

    private suspend fun getFormattedFileCount(): String {
        val contentCount = getContentCount()
        return getFormattedFileCount(contentCount.files, contentCount.folders)
    }
}
