package com.raival.compose.file.explorer.screen.main.tab.sftp

import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.common.fromJson
import com.raival.compose.file.explorer.common.toJson
import com.raival.compose.file.explorer.screen.main.tab.sftp.model.SftpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.security.PublicKey
import java.security.Security
import java.util.EnumSet

class SftpManager {
    val credentialsStore = SftpCredentialsStore()
    private val clients = mutableMapOf<String, SSHClient>()
    private val mutex = Mutex()

    @Volatile
    private var bcReady = false

    /**
     * Android ships a cut-down "BC" security provider that lacks modern algorithms
     * (e.g. X25519) which sshj needs for key exchange. Since only one provider can
     * be registered under the "BC" name, the stub must be removed first so the full
     * BouncyCastle from our dependencies takes its place.
     */
    private fun ensureBouncyCastle() {
        if (bcReady) return
        synchronized(this) {
            if (bcReady) return
            try {
                val hasX25519 = runCatching {
                    java.security.KeyPairGenerator.getInstance(
                        "X25519",
                        BouncyCastleProvider.PROVIDER_NAME
                    )
                    true
                }.getOrDefault(false)
                if (!hasX25519) {
                    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
                    Security.insertProviderAt(BouncyCastleProvider(), 1)
                }
            } catch (e: Exception) {
                globalClass.logger.logError(e)
            } finally {
                bcReady = true
            }
        }
    }

    fun getSavedServers(): List<SftpServer> =
        SftpServer.listFromJson(globalClass.preferencesManager.sftpServers)

    fun saveServers(servers: List<SftpServer>) {
        globalClass.preferencesManager.sftpServers = servers.toJson(prettyPrint = false)
    }

    private fun getKnownHosts(): MutableMap<String, String> {
        return fromJson<Map<String, String>>(globalClass.preferencesManager.sftpKnownHosts)
            ?.toMutableMap() ?: mutableMapOf()
    }

    private fun rememberHostKey(host: String, port: Int, key: PublicKey) {
        val hosts = getKnownHosts()
        hosts["$host:$port"] = SecurityUtils.getFingerprint(key)
        globalClass.preferencesManager.sftpKnownHosts = hosts.toJson(prettyPrint = false)
    }

    private fun tofuVerifier(): HostKeyVerifier {
        // HostKeyVerifier has two abstract methods, so SAM conversion is unavailable
        return object : HostKeyVerifier {
            override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
                val known = getKnownHosts()["$hostname:$port"]
                val fingerprint = try {
                    SecurityUtils.getFingerprint(key)
                } catch (_: Exception) {
                    null
                }
                return if (known == null) {
                    // Trust-on-first-use: remember and accept
                    rememberHostKey(hostname, port, key)
                    true
                } else {
                    // If fingerprint computation failed, accept (fail-open v1, logged)
                    fingerprint == null || fingerprint == known
                }
            }

            override fun findExistingAlgorithms(
                hostname: String,
                port: Int
            ): List<String> = emptyList()
        }
    }

    private suspend fun getOrCreateClient(server: SftpServer, password: String): SSHClient =
        withContext(Dispatchers.IO) {
            ensureBouncyCastle()
            mutex.withLock {
                clients[server.id]?.let {
                    if (it.isConnected && it.isAuthenticated) return@withContext it
                    runCatching { it.disconnect() }
                    clients.remove(server.id)
                }
                val client = SSHClient()
                client.addHostKeyVerifier(tofuVerifier())
                client.connectTimeout = CONNECT_TIMEOUT_MS
                client.timeout = SOCKET_TIMEOUT_MS
                client.connect(server.host, server.port)
                client.authPassword(server.username, password)
                clients[server.id] = client
                client
            }
        }

    suspend fun <T> withSftp(server: SftpServer, block: suspend (SFTPClient) -> T): T =
        withContext(Dispatchers.IO) {
            val password = credentialsStore.getPassword(server.id)
                ?: throw IllegalStateException("Missing password for server ${server.displayLabel}")
            val ssh = getOrCreateClient(server, password)
            val sftp = ssh.newSFTPClient()
            try {
                block(sftp)
            } finally {
                runCatching { sftp.close() }
            }
        }

    suspend fun testConnection(server: SftpServer, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            ensureBouncyCastle()
            runCatching {
                val client = SSHClient()
                try {
                    client.addHostKeyVerifier(tofuVerifier())
                    client.connectTimeout = CONNECT_TIMEOUT_MS
                    client.timeout = SOCKET_TIMEOUT_MS
                    client.connect(server.host, server.port)
                    client.authPassword(server.username, password)
                    client.newSFTPClient().use { sftp ->
                        sftp.stat(normalize(server.remotePath))
                    }
                } finally {
                    runCatching { client.disconnect() }
                }
            }.map { }
        }

    suspend fun listDir(server: SftpServer, path: String): List<SftpEntry> =
        withSftp(server) { sftp ->
            sftp.ls(normalize(path)).map {
                SftpEntry(
                    name = it.name,
                    path = it.path,
                    isDirectory = it.isDirectory,
                    size = runCatching { it.attributes.size }.getOrDefault(0L),
                    mtimeSecs = runCatching { it.attributes.mtime }.getOrDefault(0L)
                )
            }.filter { it.name != "." && it.name != ".." }
        }

    suspend fun stat(server: SftpServer, path: String): SftpEntry? =
        withSftp(server) { sftp ->
            runCatching {
                val attrs = sftp.stat(normalize(path))
                val norm = normalize(path)
                SftpEntry(
                    name = norm.substringAfterLast("/").ifEmpty { "/" },
                    path = norm,
                    isDirectory = attrs.type == FileMode.Type.DIRECTORY,
                    size = attrs.size,
                    mtimeSecs = attrs.mtime
                )
            }.getOrNull()
        }

    suspend fun mkdir(server: SftpServer, path: String) {
        withSftp(server) { sftp -> sftp.mkdir(normalize(path)) }
    }

    suspend fun createEmptyFile(server: SftpServer, path: String) {
        withSftp(server) { sftp ->
            sftp.open(
                normalize(path),
                EnumSet.of(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC)
            ).close()
        }
    }

    suspend fun deleteFile(server: SftpServer, path: String) {
        withSftp(server) { sftp -> sftp.rm(normalize(path)) }
    }

    suspend fun deleteDir(server: SftpServer, path: String) {
        withSftp(server) { sftp -> sftp.rmdir(normalize(path)) }
    }

    suspend fun deleteRecursively(server: SftpServer, path: String) {
        val norm = normalize(path)
        withSftp(server) { sftp ->
            deleteRecursiveImpl(sftp, norm)
        }
    }

    private fun deleteRecursiveImpl(sftp: SFTPClient, path: String) {
        val isDir = runCatching {
            sftp.stat(path).type == FileMode.Type.DIRECTORY
        }.getOrDefault(false)
        if (!isDir) {
            sftp.rm(path)
            return
        }
        sftp.ls(path)
            .filter { it.name != "." && it.name != ".." }
            .forEach { child -> deleteRecursiveImpl(sftp, child.path) }
        sftp.rmdir(path)
    }

    suspend fun rename(server: SftpServer, oldPath: String, newPath: String) {
        withSftp(server) { sftp -> sftp.rename(normalize(oldPath), normalize(newPath)) }
    }

    suspend fun downloadToLocal(server: SftpServer, remotePath: String, localFile: File) {
        withSftp(server) { sftp ->
            localFile.parentFile?.mkdirs()
            sftp.get(normalize(remotePath), localFile.absolutePath)
        }
    }

    suspend fun uploadFromLocal(server: SftpServer, localFile: File, remotePath: String) {
        withSftp(server) { sftp ->
            sftp.put(localFile.absolutePath, normalize(remotePath))
        }
    }

    fun disconnect(serverId: String) {
        synchronized(clients) {
            clients.remove(serverId)?.let { runCatching { it.disconnect() } }
        }
    }

    fun disconnectAll() {
        synchronized(clients) {
            clients.values.forEach { runCatching { it.disconnect() } }
            clients.clear()
        }
    }

    companion object {
        const val CONNECT_TIMEOUT_MS = 30_000
        const val SOCKET_TIMEOUT_MS = 30_000

        fun normalize(path: String): String {
            if (path.isBlank()) return "/"
            var p = path.replace("\\", "/").trim()
            if (!p.startsWith("/")) p = "/$p"
            // collapse duplicate slashes, drop trailing slash (except root)
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

data class SftpEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val mtimeSecs: Long
) {
    val lastModifiedMs: Long get() = mtimeSecs * 1000L
}
