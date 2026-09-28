package com.raival.compose.file.explorer.screen.main.model

import com.google.gson.annotations.SerializedName
import java.util.Date

data class GithubReleaseAsset(
    @SerializedName("name")
    val name: String,
    @SerializedName("browser_download_url")
    val browserDownloadUrl: String
)

data class GithubRelease(
    @SerializedName("html_url")
    val htmlUrl: String,
    @SerializedName("tag_name")
    val tagName: String,
    @SerializedName("body")
    val body: String,
    @SerializedName("published_at")
    val publishedAt: Date,
    @SerializedName("prerelease")
    val prerelease: Boolean = false,
    @SerializedName("assets")
    val assets: List<GithubReleaseAsset>
) {
    /**
     * Picks the APK matching this install type: debug builds (`.debug` suffix)
     * get the `app-debug.apk` asset, release builds get the first non-debug APK.
     */
    fun preferredAsset(isDebugInstall: Boolean): GithubReleaseAsset? {
        val apks = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        if (apks.isEmpty()) return null
        return if (isDebugInstall) {
            apks.firstOrNull { it.name.contains("debug", ignoreCase = true) }
                ?: apks.first()
        } else {
            apks.firstOrNull { !it.name.contains("debug", ignoreCase = true) }
                ?: apks.first()
        }
    }
}