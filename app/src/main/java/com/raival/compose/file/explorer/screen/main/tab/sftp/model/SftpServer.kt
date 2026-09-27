package com.raival.compose.file.explorer.screen.main.tab.sftp.model

import com.raival.compose.file.explorer.common.emptyString
import com.raival.compose.file.explorer.common.fromJson
import com.raival.compose.file.explorer.common.toJson
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SftpServer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = emptyString,
    val host: String = emptyString,
    val port: Int = 22,
    val username: String = emptyString,
    val remotePath: String = "/"
) {
    val displayLabel: String
        get() = if (name.isNotBlank()) name else "$username@$host"

    fun toSftpRootPath(): String = "sftp://$id$remotePath"

    companion object {
        fun listFromJson(json: String?): List<SftpServer> {
            if (json.isNullOrBlank()) return emptyList()
            return fromJson<List<SftpServer>>(json) ?: emptyList()
        }

        fun List<SftpServer>.toJsonString(): String = toJson(prettyPrint = false)
    }
}
