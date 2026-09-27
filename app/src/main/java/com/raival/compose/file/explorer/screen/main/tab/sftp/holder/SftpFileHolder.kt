package com.raival.compose.file.explorer.screen.main.tab.sftp.holder

import android.content.Context
import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.common.emptyString
import com.raival.compose.file.explorer.screen.main.tab.files.holder.ContentHolder
import com.raival.compose.file.explorer.screen.main.tab.files.holder.LocalFileHolder
import com.raival.compose.file.explorer.screen.main.tab.files.misc.ContentCount
import com.raival.compose.file.explorer.screen.main.tab.sftp.SftpManager
import com.raival.compose.file.explorer.screen.main.tab.sftp.model.SftpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SftpFileHolder(
    val server: SftpServer,
    val remotePath: String = "/"
) : ContentHolder() {

    private val manager: SftpManager
        get() = globalClass.sftpManager

    private var cachedIsDir: Boolean? = null
    private var cachedSize: Long? = null
    private var cachedMtime: Long? = null

    val normalizedPath: String
        get() = SftpManager.normalize(remotePath)

    override val uniquePath: String
        get() = "sftp://${server.id}$normalizedPath"

    override val displayName: String
        get() = if (normalizedPath == "/") server.displayLabel else normalizedPath.substringAfterLast("/")

    override val isFolder: Boolean
        get() = cachedIsDir ?: true

    override val lastModified: Long
        get() = cachedMtime ?: 0L

    override val size: Long
        get() = cachedSize ?: 0L

    override val extension: String
        get() = if (isFolder) emptyString else displayName.substringAfterLast(".", emptyString).lowercase()

    override val canRead: Boolean = true
    override val canWrite: Boolean = true
    override val canAddNewContent: Boolean = true

    override suspend fun listContent(): ArrayList<out ContentHolder> =
        withContext(Dispatchers.IO) {
            val entries = manager.listDir(server, normalizedPath)
            ArrayList(entries.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                .map {
                    SftpFileHolder(server, it.path).apply {
                        cachedIsDir = it.isDirectory
                        cachedSize = it.size
                        cachedMtime = it.lastModifiedMs
                    }
                })
        }

    override suspend fun getParent(): ContentHolder? =
        withContext(Dispatchers.IO) {
            SftpManager.parentOf(normalizedPath)?.let { SftpFileHolder(server, it) }
        }

    override suspend fun getContentCount(): ContentCount =
        withContext(Dispatchers.IO) {
            var files = 0
            var folders = 0
            runCatching { manager.listDir(server, normalizedPath) }.getOrNull()
                ?.forEach { if (it.isDirectory) folders++ else files++ }
            ContentCount(files = files, folders = folders)
        }

    override suspend fun findFile(name: String): ContentHolder? =
        withContext(Dispatchers.IO) {
            val child = SftpManager.join(normalizedPath, name)
            runCatching { manager.stat(server, child) }.getOrNull()?.let {
                SftpFileHolder(server, child).apply {
                    cachedIsDir = it.isDirectory
                    cachedSize = it.size
                    cachedMtime = it.lastModifiedMs
                }
            }
        }

    override suspend fun isValid(): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { manager.stat(server, normalizedPath) }.getOrNull()?.let {
                cachedIsDir = it.isDirectory
                cachedSize = it.size
                cachedMtime = it.lastModifiedMs
                true
            } ?: false
        }

    override suspend fun createSubFile(name: String, onCreated: (ContentHolder?) -> Unit) {
        withContext(Dispatchers.IO) {
            val child = SftpManager.join(normalizedPath, name)
            val ok = runCatching { manager.createEmptyFile(server, child) }.isSuccess
            withContext(Dispatchers.Main) {
                onCreated(if (ok) SftpFileHolder(server, child) else null)
            }
        }
    }

    override suspend fun createSubFolder(name: String, onCreated: (ContentHolder?) -> Unit) {
        withContext(Dispatchers.IO) {
            val child = SftpManager.join(normalizedPath, name)
            val ok = runCatching { manager.mkdir(server, child) }.isSuccess
            withContext(Dispatchers.Main) {
                onCreated(if (ok) SftpFileHolder(server, child) else null)
            }
        }
    }

    override fun open(
        context: Context,
        anonymous: Boolean,
        skipSupportedExtensions: Boolean,
        customMimeType: String?
    ) {
        // Download to cache then delegate to LocalFileHolder so all viewers keep working.
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            downloadAndOpen(context, skipSupportedExtensions, customMimeType)
        }
    }

    suspend fun downloadAndOpen(
        context: Context,
        skipSupportedExtensions: Boolean,
        customMimeType: String?
    ) {
        withContext(Dispatchers.IO) {
            try {
                val tmp = File(
                    File(globalClass.cacheDir, "sftp_preview/${server.id}"),
                    normalizedPath.trimStart('/')
                ).apply { parentFile?.mkdirs() }
                manager.downloadToLocal(server, normalizedPath, tmp)
                withContext(Dispatchers.Main) {
                    LocalFileHolder(tmp).open(
                        context = context,
                        anonymous = false,
                        skipSupportedExtensions = skipSupportedExtensions,
                        customMimeType = customMimeType
                    )
                }
            } catch (e: Exception) {
                globalClass.logger.logError(e)
                withContext(Dispatchers.Main) {
                    globalClass.showMsg(e.message ?: "Failed to open remote file")
                }
            }
        }
    }

    suspend fun downloadTo(localFile: File) {
        manager.downloadToLocal(server, normalizedPath, localFile)
    }

    suspend fun deleteRemote(recursive: Boolean = true) {
        if (recursive) manager.deleteRecursively(server, normalizedPath)
        else if (isFolder) manager.deleteDir(server, normalizedPath)
        else manager.deleteFile(server, normalizedPath)
    }

    suspend fun renameRemote(newName: String): SftpFileHolder? {
        val parent = SftpManager.parentOf(normalizedPath) ?: return null
        val dest = SftpManager.join(parent, newName)
        return runCatching {
            manager.rename(server, normalizedPath, dest)
            SftpFileHolder(server, dest)
        }.getOrNull()
    }

    override suspend fun getDetails(): String {
        val entry = runCatching { manager.stat(server, normalizedPath) }.getOrNull()
        return buildString {
            append(displayName)
            if (entry != null && !entry.isDirectory) {
                append(" | ${entry.size} B")
            }
            append(" | sftp://${server.host}:${server.port}$normalizedPath")
        }
    }
}
