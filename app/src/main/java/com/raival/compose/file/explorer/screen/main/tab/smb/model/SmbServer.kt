package com.raival.compose.file.explorer.screen.main.tab.smb.model

import com.google.gson.annotations.SerializedName
import com.raival.compose.file.explorer.common.emptyString
import com.raival.compose.file.explorer.common.toJson
import java.util.UUID

enum class SmbAuthType {
    @SerializedName("guest")
    GUEST,
    @SerializedName("user")
    USER
}

data class SmbServer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = emptyString,
    val host: String = emptyString,
    val share: String = emptyString,
    val username: String? = null,
    val authType: SmbAuthType = SmbAuthType.GUEST
) {
    val displayLabel: String
        get() = name.ifBlank { "$host/$share" }

    companion object {
        fun listFromJson(json: String?): List<SmbServer> {
            if (json.isNullOrBlank()) return emptyList()
            return runCatching {
                com.raival.compose.file.explorer.common.fromJson<List<SmbServer>>(json)
                    ?: emptyList()
            }.getOrDefault(emptyList())
        }

        fun toJson(servers: List<SmbServer>, prettyPrint: Boolean = false): String =
            servers.toJson(prettyPrint)
    }
}