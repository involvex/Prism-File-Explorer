package com.raival.compose.file.explorer.screen.main.tab.smb

import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.common.emptyString
import com.raival.compose.file.explorer.common.fromJson
import com.raival.compose.file.explorer.common.toJson
import com.raival.compose.file.explorer.screen.main.tab.smb.model.SmbAuthType
import com.raival.compose.file.explorer.screen.main.tab.smb.model.SmbServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.EnumSet
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileAllInformation
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.msfscc.fileinformation.FileStandardInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.msdtyp.FileTime
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.Directory
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File as SmbFile

class SmbManager {
    val credentialsStore = SmbCredentialsStore()
    private val clients = mutableMapOf<String, Connection>()
    private val mutex = Mutex()

    fun getSavedServers(): List<SmbServer> =
        SmbServer.listFromJson(globalClass.preferencesManager.smbServers)

    fun saveServers(servers: List<SmbServer>) {
        globalClass.preferencesManager.smbServers = servers.toJson(prettyPrint = false)
    }

    suspend fun hasUsableCredential(server: SmbServer): Boolean =
        when (server.authType) {
            SmbAuthType.GUEST -> true
            SmbAuthType.USER -> credentialsStore.hasPassword(server.id)
        }

    private fun buildAuth(server: SmbServer, password: String?): AuthenticationContext {
        return if (server.authType == SmbAuthType.GUEST) {
            AuthenticationContext.guest()
        } else {
            val user = server.username ?: "guest"
            val pw = password?.toCharArray() ?: CharArray(0)
            AuthenticationContext(user, pw, "")
        }
    }

    private suspend fun getOrCreateConnection(
        server: SmbServer,
        password: String? = null
    ): Connection = withContext(Dispatchers.IO) {
        mutex.withLock {
            clients[server.id]?.let { conn ->
                if (conn.isConnected) return@withLock conn
                runCatching { conn.close() }
                clients.remove(server.id)
            }
            val client = SMBClient()
            val conn = client.connect(server.host, DEFAULT_PORT)
            clients[server.id] = conn
            conn
        }
    }

    private suspend fun <T> getShare(
        server: SmbServer,
        password: String? = null,
        block: (DiskShare) -> T
    ): T = withContext(Dispatchers.IO) {
        val conn = getOrCreateConnection(server, password ?: resolvedPassword(server))
        val session = conn.authenticate(buildAuth(server, password ?: resolvedPassword(server)))
        val share = session.connectShare(server.share) as DiskShare
        try {
            block(share)
        } finally {
            runCatching { share.close() }
        }
    }

    private suspend fun resolvedPassword(server: SmbServer): String? =
        if (server.authType == SmbAuthType.GUEST) null else credentialsStore.getPassword(server.id)

    private fun mkdirs(share: DiskShare, path: String) {
        val norm = normalize(path)
        if (norm == "/" || norm.isEmpty()) return
        val parts = norm.trimStart('/').split("/")
        var cur = ""
        for (part in parts) {
            cur = if (cur.isEmpty()) "/$part" else "$cur/$part"
            if (!share.folderExists(cur)) {
                runCatching { share.mkdir(cur) }
            }
        }
    }

    private fun readAccess(): Set<AccessMask> = setOf(
        AccessMask.FILE_READ_DATA,
        AccessMask.FILE_READ_ATTRIBUTES,
        AccessMask.SYNCHRONIZE
    )

    private fun listAccess(): Set<AccessMask> = setOf(
        AccessMask.FILE_LIST_DIRECTORY,
        AccessMask.FILE_READ_ATTRIBUTES,
        AccessMask.SYNCHRONIZE
    )

    private fun writeAccess(): Set<AccessMask> = setOf(
        AccessMask.FILE_WRITE_DATA,
        AccessMask.FILE_APPEND_DATA,
        AccessMask.FILE_WRITE_ATTRIBUTES,
        AccessMask.SYNCHRONIZE
    )

    private fun fullAccess(): Set<AccessMask> = setOf(
        AccessMask.FILE_READ_DATA,
        AccessMask.FILE_WRITE_DATA,
        AccessMask.FILE_APPEND_DATA,
        AccessMask.FILE_LIST_DIRECTORY,
        AccessMask.FILE_READ_ATTRIBUTES,
        AccessMask.FILE_WRITE_ATTRIBUTES,
        AccessMask.DELETE,
        AccessMask.SYNCHRONIZE
    )

    private fun shareAccess(): Set<SMB2ShareAccess> = SMB2ShareAccess.ALL

    private fun createOptions(isDirectory: Boolean): Set<SMB2CreateOptions> =
        if (isDirectory) setOf(SMB2CreateOptions.FILE_DIRECTORY_FILE)
        else setOf(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE)

    private fun openRead(share: DiskShare, path: String): SmbFile = share.openFile(
        path,
        readAccess(),
        EnumSet.noneOf(FileAttributes::class.java),
        shareAccess(),
        SMB2CreateDisposition.FILE_OPEN,
        createOptions(false)
    )

    private fun openWrite(share: DiskShare, path: String, create: Boolean): SmbFile = share.openFile(
        path,
        writeAccess(),
        EnumSet.noneOf(FileAttributes::class.java),
        shareAccess(),
        if (create) SMB2CreateDisposition.FILE_CREATE else SMB2CreateDisposition.FILE_OVERWRITE_IF,
        createOptions(false)
    )

    private fun openDirectory(share: DiskShare, path: String): Directory = share.openDirectory(
        path,
        listAccess(),
        EnumSet.noneOf(FileAttributes::class.java),
        shareAccess(),
        SMB2CreateDisposition.FILE_OPEN,
        createOptions(true)
    )

    private fun toEntry(info: FileIdBothDirectoryInformation, parent: String): SmbEntry {
        val name = info.getFileName()
        val isDir = (info.getFileAttributes().toInt() and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.getValue().toInt()) != 0
        val size = info.getEndOfFile()
        val mtime = info.getLastWriteTime().toEpochMillis()
        return SmbEntry(
            name = name,
            path = join(parent, name),
            isDirectory = isDir,
            size = size,
            lastModifiedMs = mtime
        )
    }

    private fun toEntry(info: FileAllInformation, path: String): SmbEntry {
        val norm = normalize(path)
        val name = norm.substringAfterLast("/").ifEmpty { "/" }
        val std = info.getStandardInformation()
        val isDir = std.isDirectory
        val size = std.getEndOfFile()
        val basic = info.getBasicInformation()
        val mtime = basic.getLastWriteTime().toEpochMillis()
        return SmbEntry(
            name = name,
            path = norm,
            isDirectory = isDir,
            size = size,
            lastModifiedMs = mtime
        )
    }

    suspend fun listDir(server: SmbServer, path: String): List<SmbEntry> =
        withContext(Dispatchers.IO) {
            val norm = normalize(path)
            val entries = ArrayList<SmbEntry>()
            getShare(server) { share ->
                share.list(norm).forEach { info ->
                    entries.add(toEntry(info, norm))
                }
            }
            entries
        }

    suspend fun stat(server: SmbServer, path: String): SmbEntry? =
        withContext(Dispatchers.IO) {
            val norm = normalize(path)
            runCatching {
                getShare(server) { share ->
                    toEntry(share.getFileInformation(norm), norm)
                }
            }.getOrNull()
        }

    suspend fun mkdir(server: SmbServer, path: String) {
        withContext(Dispatchers.IO) {
            getShare(server) { share -> share.mkdir(normalize(path)) }
        }
    }

    suspend fun createEmptyFile(server: SmbServer, path: String) {
        withContext(Dispatchers.IO) {
            getShare(server) { share ->
                openWrite(share, normalize(path), create = true).close()
            }
        }
    }

    suspend fun deleteFile(server: SmbServer, path: String) {
        withContext(Dispatchers.IO) {
            getShare(server) { share -> share.rm(normalize(path)) }
        }
    }

    suspend fun deleteDir(server: SmbServer, path: String) {
        withContext(Dispatchers.IO) {
            getShare(server) { share -> share.rmdir(normalize(path), false) }
        }
    }

    suspend fun deleteRecursively(server: SmbServer, path: String) {
        val norm = normalize(path)
        withContext(Dispatchers.IO) {
            getShare(server) { share ->
                deleteRecursiveImpl(share, norm)
            }
        }
    }

    private fun deleteRecursiveImpl(share: DiskShare, path: String) {
        val isDir = runCatching {
            share.getFileInformation(path).getStandardInformation().isDirectory
        }.getOrDefault(false)
        if (!isDir) {
            runCatching { share.rm(path) }
            return
        }
        share.list(path).forEach { child ->
            deleteRecursiveImpl(share, join(path, child.getFileName()))
        }
        runCatching { share.rmdir(path, false) }
    }

    suspend fun rename(server: SmbServer, oldPath: String, newPath: String) {
        withContext(Dispatchers.IO) {
            getShare(server) { share ->
                val old = normalize(oldPath)
                val new = normalize(newPath)
                val entry = share.openFile(
                    old,
                    fullAccess(),
                    EnumSet.noneOf(FileAttributes::class.java),
                    shareAccess(),
                    SMB2CreateDisposition.FILE_OPEN,
                    createOptions(false)
                )
                entry.use { it.rename(new.substringAfterLast("/")) }
            }
        }
    }

    suspend fun downloadToLocal(server: SmbServer, remotePath: String, localFile: File) {
        withContext(Dispatchers.IO) {
            localFile.parentFile?.mkdirs()
            val norm = normalize(remotePath)
            getShare(server) { share ->
                val remote = openRead(share, norm)
                remote.use { file ->
                    file.getInputStream().use { input ->
                        localFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    suspend fun uploadFromLocal(server: SmbServer, localFile: File, remotePath: String) {
        withContext(Dispatchers.IO) {
            val norm = normalize(remotePath)
            val parent = parentOf(norm)
            if (parent != null) {
                getShare(server) { share ->
                    runCatching { mkdirs(share, parent) }
                }
            }
            getShare(server) { share ->
                val remote = openWrite(share, norm, create = true)
                remote.use { file ->
                    localFile.inputStream().use { input ->
                        file.getOutputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    suspend fun testConnection(
        server: SmbServer,
        password: String? = null
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
runCatching {
                    val client = SMBClient()
                    try {
                        val conn = client.connect(server.host, DEFAULT_PORT)
                        val auth = buildAuth(server, password)
                        val session = conn.authenticate(auth)
                        val share = session.connectShare(server.share) as DiskShare
                        share.getFileInformation(normalize("/"))
                        conn.close()
                    } finally {
                        runCatching { client.close() }
                    }
                }.map { }
        }

    fun disconnect(serverId: String) {
        synchronized(clients) {
            clients.remove(serverId)?.let { runCatching { it.close() } }
        }
    }

    fun disconnectAll() {
        synchronized(clients) {
            clients.values.forEach { runCatching { it.close() } }
            clients.clear()
        }
    }

    companion object {
        const val DEFAULT_PORT = 445

        fun normalize(path: String): String {
            if (path.isBlank()) return "/"
            var p = path.trim()
            if (!p.startsWith("/")) p = "/$p"
            p = p.replace(Regex("/+"), "/")
            if (p.length > 1 && p.endsWith("/")) p = p.dropLast(1)
            return p
        }

        fun join(parent: String, child: String): String {
            val p = normalize(parent)
            return if (p == "/") "/$child" else "$p/$child"
        }

        fun parentOf(path: String): String? {
            val p = normalize(path)
            if (p == "/") return null
            val parent = p.substringBeforeLast("/")
            return parent.ifEmpty { "/" }
        }
    }
}

data class SmbEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModifiedMs: Long
)
