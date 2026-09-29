package com.raival.compose.file.explorer.screen.main.tab.files.holder

import android.content.Context
import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.R
import com.raival.compose.file.explorer.common.emptyString
import com.raival.compose.file.explorer.screen.main.tab.files.holder.LocalFileHolder
import com.raival.compose.file.explorer.screen.main.tab.files.misc.ContentCount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

abstract class RemoteFileHolder : ContentHolder() {

    private var cachedIsDir: Boolean? = null
    private var cachedSize: Long? = null
    private var cachedMtime: Long? = null

    abstract val server: Any
    abstract val remotePath: String

    abstract fun serverId(): String
    abstract fun serverLabel(): String

    abstract fun getManager(): Any
    abstract fun normalize(path: String): String
    abstract fun join(parent: String, child: String): String
    abstract fun parentOf(path: String): String?
    abstract val previewDirName: String
    abstract val protocolPrefix: String

    abstract fun createChild(server: Any, path: String): RemoteFileHolder

    abstract suspend fun listEntries(server: Any, path: String): List<RemoteEntry>
    abstract suspend fun statEntry(server: Any, path: String): RemoteEntry?
    abstract suspend fun mkdir(server: Any, path: String)
    abstract suspend fun createEmptyFile(server: Any, path: String)
    abstract suspend fun deleteFile(server: Any, path: String)
    abstract suspend fun deleteDir(server: Any, path: String)
    abstract suspend fun deleteRecursively(server: Any, path: String)
    abstract suspend fun rename(server: Any, oldPath: String, newPath: String)
    abstract suspend fun downloadToLocal(server: Any, remotePath: String, localFile: File)
    abstract fun buildDetailsString(entry: RemoteEntry?): String

    val normalizedPath: String
        get() = normalize(remotePath)

    override val uniquePath: String
        get() = "${protocolPrefix}://${serverId()}$normalizedPath"

    override val displayName: String
        get() = if (normalizedPath == "/") serverLabel() else normalizedPath.substringAfterLast("/")

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
            val entries = listEntries(server, normalizedPath)
            ArrayList(entries.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                .map {
                    createChild(server, it.path).apply {
                        cachedIsDir = it.isDirectory
                        cachedSize = it.size
                        cachedMtime = it.lastModifiedMs
                    }
                })
        }

    override suspend fun getParent(): ContentHolder? =
        withContext(Dispatchers.IO) {
            parentOf(normalizedPath)?.let { createChild(server, it) }
        }

    override suspend fun getContentCount(): ContentCount =
        withContext(Dispatchers.IO) {
            var files = 0
            var folders = 0
            runCatching { listEntries(server, normalizedPath) }.getOrNull()
                ?.forEach { if (it.isDirectory) folders++ else files++ }
            ContentCount(files = files, folders = folders)
        }

    override suspend fun findFile(name: String): ContentHolder? =
        withContext(Dispatchers.IO) {
            val child = join(normalizedPath, name)
            runCatching { statEntry(server, child) }.getOrNull()?.let {
                createChild(server, child).apply {
                    cachedIsDir = it.isDirectory
                    cachedSize = it.size
                    cachedMtime = it.lastModifiedMs
                }
            }
        }

    override suspend fun isValid(): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { statEntry(server, normalizedPath) }.getOrNull()?.let {
                cachedIsDir = it.isDirectory
                cachedSize = it.size
                cachedMtime = it.lastModifiedMs
                true
            } ?: false
        }

    override suspend fun createSubFile(name: String, onCreated: (ContentHolder?) -> Unit) {
        withContext(Dispatchers.IO) {
            val child = join(normalizedPath, name)
            val ok = runCatching { createEmptyFile(server, child) }.isSuccess
            withContext(Dispatchers.Main) {
                onCreated(if (ok) createChild(server, child) else null)
            }
        }
    }

    override suspend fun createSubFolder(name: String, onCreated: (ContentHolder?) -> Unit) {
        withContext(Dispatchers.IO) {
            val child = join(normalizedPath, name)
            val ok = runCatching { mkdir(server, child) }.isSuccess
            withContext(Dispatchers.Main) {
                onCreated(if (ok) createChild(server, child) else null)
            }
        }
    }

    override fun open(
        context: Context,
        anonymous: Boolean,
        skipSupportedExtensions: Boolean,
        customMimeType: String?
    ) {
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
                    File(globalClass.cacheDir, "${previewDirName}/${serverId()}"),
                    normalizedPath.trimStart('/')
                ).apply { parentFile?.mkdirs() }
                downloadToLocal(server, normalizedPath, tmp)
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
                    globalClass.showMsg(e.message ?: globalClass.getString(R.string.failed_to_open_remote_file))
                }
            }
        }
    }

    suspend fun downloadTo(localFile: File) {
        downloadToLocal(server, normalizedPath, localFile)
    }

    suspend fun deleteRemote(recursive: Boolean = true) {
        if (recursive) deleteRecursively(server, normalizedPath)
        else if (isFolder) deleteDir(server, normalizedPath)
        else deleteFile(server, normalizedPath)
    }

    open suspend fun renameRemote(newName: String): RemoteFileHolder? {
        val parent = parentOf(normalizedPath) ?: return null
        val dest = join(parent, newName)
        return runCatching {
            rename(server, normalizedPath, dest)
            createChild(server, dest)
        }.getOrNull()
    }

    override suspend fun getDetails(): String {
        val entry = runCatching { statEntry(server, normalizedPath) }.getOrNull()
        return buildDetailsString(entry)
    }
}

data class RemoteEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModifiedMs: Long
)