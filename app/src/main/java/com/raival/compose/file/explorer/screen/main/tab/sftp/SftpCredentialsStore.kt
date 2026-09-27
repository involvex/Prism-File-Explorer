package com.raival.compose.file.explorer.screen.main.tab.sftp

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.raival.compose.file.explorer.App.Companion.globalClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SftpCredentialsStore {
    private var cachedPrefs: SharedPreferences? = null

    private fun prefs(): SharedPreferences {
        cachedPrefs?.let { return it }
        val masterKey = MasterKey.Builder(globalClass)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            globalClass,
            "sftp_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).also { cachedPrefs = it }
    }

    suspend fun savePassword(serverId: String, password: String) =
        withContext(Dispatchers.IO) {
            prefs().edit().putString(passwordKey(serverId), password).apply()
        }

    suspend fun getPassword(serverId: String): String? = withContext(Dispatchers.IO) {
        try {
            prefs().getString(passwordKey(serverId), null)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun clear(serverId: String) = withContext(Dispatchers.IO) {
        prefs().edit().remove(passwordKey(serverId)).apply()
    }

    suspend fun hasPassword(serverId: String): Boolean = getPassword(serverId) != null

    private fun passwordKey(serverId: String) = "sftp_pw_$serverId"
}
