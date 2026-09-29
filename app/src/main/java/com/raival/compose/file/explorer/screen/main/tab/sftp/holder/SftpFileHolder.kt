package com.raival.compose.file.explorer.screen.main.tab.sftp.holder

import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.screen.main.tab.sftp.SftpManager
import com.raival.compose.file.explorer.screen.main.tab.sftp.model.SftpServer
import com.raival.compose.file.explorer.screen.main.tab.files.holder.RemoteEntry
import com.raival.compose.file.explorer.screen.main.tab.files.holder.RemoteFileHolder
import java.io.File

class SftpFileHolder(
    override val server: SftpServer,
    override val remotePath: String = "/"
) : RemoteFileHolder() {

    private val manager: SftpManager
        get() = globalClass.sftpManager

    override fun getManager(): Any = manager

    override fun serverId(): String = server.id

    override fun serverLabel(): String = server.displayLabel

    override fun normalize(path: String): String = SftpManager.normalize(path)

    override fun join(parent: String, child: String): String = SftpManager.join(parent, child)

    override fun parentOf(path: String): String? = SftpManager.parentOf(path)

    override val previewDirName: String = "sftp_preview"

    override val protocolPrefix: String = "sftp"

    override fun createChild(server: Any, path: String): RemoteFileHolder =
        SftpFileHolder(server as SftpServer, path)

    override suspend fun listEntries(server: Any, path: String): List<RemoteEntry> =
        manager.listDir(server as SftpServer, path).map {
            RemoteEntry(
                name = it.name,
                path = it.path,
                isDirectory = it.isDirectory,
                size = it.size,
                lastModifiedMs = it.lastModifiedMs
            )
        }

    override suspend fun statEntry(server: Any, path: String): RemoteEntry? =
        manager.stat(server as SftpServer, path)?.let {
            RemoteEntry(
                name = it.name,
                path = it.path,
                isDirectory = it.isDirectory,
                size = it.size,
                lastModifiedMs = it.lastModifiedMs
            )
        }

    override suspend fun mkdir(server: Any, path: String) =
        manager.mkdir(server as SftpServer, path)

    override suspend fun createEmptyFile(server: Any, path: String) =
        manager.createEmptyFile(server as SftpServer, path)

    override suspend fun deleteFile(server: Any, path: String) =
        manager.deleteFile(server as SftpServer, path)

    override suspend fun deleteDir(server: Any, path: String) =
        manager.deleteDir(server as SftpServer, path)

    override suspend fun deleteRecursively(server: Any, path: String) =
        manager.deleteRecursively(server as SftpServer, path)

    override suspend fun rename(server: Any, oldPath: String, newPath: String) =
        manager.rename(server as SftpServer, oldPath, newPath)

    override suspend fun downloadToLocal(server: Any, remotePath: String, localFile: File) =
        manager.downloadToLocal(server as SftpServer, remotePath, localFile)

    override fun buildDetailsString(entry: RemoteEntry?): String = buildString {
        append(displayName)
        if (entry != null && !entry.isDirectory) {
            append(" | ${entry.size} B")
        }
        append(" | sftp://${server.host}:${server.port}$normalizedPath")
    }

    override suspend fun renameRemote(newName: String): SftpFileHolder? {
        val parent = parentOf(normalizedPath) ?: return null
        val dest = join(parent, newName)
        return runCatching {
            rename(server, normalizedPath, dest)
            SftpFileHolder(server, dest)
        }.getOrNull()
    }
}