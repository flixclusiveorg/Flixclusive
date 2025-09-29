package com.flixclusive.core.network.retrofit.dto

import com.flixclusive.core.common.config.PlatformType
import com.google.gson.annotations.SerializedName

data class GithubRelease(
    @SerializedName("body") val releaseNotes: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("tag_name") val tagName: String,
    private val assets: List<GithubAsset>,
    val name: String,
) {
    /**
     * Returns the download URL for the APK corresponding to the specified [platform].
     *
     * @param platform The platform type (MOBILE or TV) for which to get the download URL.
     *
     * @return The download URL of the APK if found, otherwise null.
     * */
    fun getDownloadUrl(platform: PlatformType): String? {
        val apk = when (platform) {
            PlatformType.MOBILE -> assets.firstOrNull { it.isApk && it.name.endsWith("mobile") }
            PlatformType.TV -> assets.firstOrNull { it.isApk && it.name.endsWith("tv") }
        }

        return apk?.downloadUrl
    }

    companion object {
        data class GithubAsset(
            @SerializedName("browser_download_url") val downloadUrl: String,
            @SerializedName("name") val name: String,
            @SerializedName("content_type") val contentType: String,
        ) {
            val isApk: Boolean get() = contentType == APK_CONTENT_TYPE
        }

        internal const val APK_CONTENT_TYPE = "application/vnd.android.package-archive"
    }
}
