package com.raival.compose.file.explorer.screen.main.tab.smb.holder

import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.R
import com.raival.compose.file.explorer.screen.main.tab.smb.SmbManager
import com.raival.compose.file.explorer.screen.main.tab.smb.model.SmbServer
import com.raival.compose.file.explorer.screen.main.tab.files.holder.RemoteEntry
import com.raival.compose.file.explorer.screen.main.tab.files.holder.RemoteFileHolder
import java.io.File

class SmbFileHolder(
    override val server: SmbServer,
    override val remotePath: String = "/"
) : RemoteFileHolder() {

    private val manager: SmbManager
        get() = globalClass.smbManager

    override fun getManager(): Any = manager

    override fun serverId(): String = server.id

    override fun serverLabel(): String = server.displayLabel

    override fun normalize(path: String): String = SmbManager.normalize(path)

    override fun join(parent: String, child: String): String = SmbManager.join(parent, child)

    override fun parentOf(path: String): String? = SmbManager.parentOf(path)

    override val previewDirName: String = "smb_preview"

    override val protocolPrefix: String = "smb"

    override fun createChild(server: Any, path: String): RemoteFileHolder =
        SmbFileHolder(server as SmbServer, path)

    override suspend fun listEntries(server: Any, path: String): List<RemoteEntry> =
        manager.listDir(server as SmbServer, path).map {
            RemoteEntry(
                name = it.name,
                path = it.path,
                isDirectory = it.isDirectory,
                size = it.size,
                lastModifiedMs = it.lastModifiedMs
            )
        }

    override suspend fun statEntry(server: Any, path: String): RemoteEntry? =
        manager.stat(server as SmbServer, path)?.let {
            RemoteEntry(
                name = it.name,
                path = it.path,
                isDirectory = it.isDirectory,
                size = it.size,
                lastModifiedMs = it.lastModifiedMs
            )
        }

    override suspend fun mkdir(server: Any, path: String) =
        manager.mkdir(server as SmbServer, path)

    override suspend fun createEmptyFile(server: Any, path: String) =
        manager.createEmptyFile(server as SmbServer, path)

    override suspend fun deleteFile(server: Any, path: String) =
        manager.deleteFile(server as SmbServer, path)

    override suspend fun deleteDir(server: Any, path: String) =
        manager.deleteDir(server as SmbServer, path)

    override suspend fun deleteRecursively(server: Any, path: String) =
        manager.deleteRecursively(server as SmbServer, path)

    override suspend fun rename(server: Any, oldPath: String, newPath: String) =
        manager.rename(server as SmbServer, oldPath, newPath)

    override suspend fun downloadToLocal(server: Any, remotePath: String, localFile: File) =
        manager.downloadToLocal(server as SmbServer, remotePath, localFile)

    override fun buildDetailsString(entry: RemoteEntry?): String = buildString {
        append(displayName)
        if (entry != null && !entry.isDirectory) {
            append(globalClass.getString(R.string.smb_details_size, entry.size))
        }
        append(
            globalClass.getString(
                R.string.smb_details_path,
                server.host,
                SmbManager.DEFAULT_PORT,
                normalizedPath
            )
        )
    }
}