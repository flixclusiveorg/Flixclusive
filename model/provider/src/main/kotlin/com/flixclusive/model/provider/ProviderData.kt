package com.flixclusive.model.provider

import com.flixclusive.gradle.entities.Author
import com.flixclusive.gradle.entities.Language
import kotlinx.serialization.Serializable

/**
 * Represents the data associated with a provider.
 *
 * @property authors The list of [Author]s who contributed to the provider.
 * @property repositoryUrl The main repository URL of the provider, if available.
 * @property buildUrl The URL for downloading the provider build.
 * @property changelog The changelog of the provider, if available. Supports Markdown.
 * @property versionName The version name of the provider.
 * @property versionCode The version code of the provider.
 * @property description The description of the provider. Supports Markdown.
 * @property iconUrl The URL to the icon/image associated with the provider, if available.
 * @property language The primary [Language] supported by this provider.
 * @property name The name of the provider.
 * @property providerType The [ProviderType] of the provider.
 * @property status The [Status] of the provider.
 *
 * @see Status
 * @see Language
 * @see ProviderType
 * @see Author
 */
@Serializable
data class ProviderData(
    val authors: List<Author>,
    val repositoryUrl: String?,
    val buildUrl: String?,
    val changelog: String? = null,
    val versionName: String,
    val versionCode: Long,
    // ==================== \\
    val description: String?,
    val iconUrl: String?,
    val language: Language,
    val name: String,
    val providerType: ProviderType,
    val status: Status,
) {
    /**
     *
     * Gets the unique identifier for the provider as
     * name alone is not reliable enough to determine
     * the uniqueness of a provider.
     *
     * */
    val id: String
        get() {
            val repositoryName = if (repositoryUrl != null) {
                "-" + getRepositoryNameFromUrl(repositoryUrl)
            } else ""

            return "$name$repositoryName-$versionCode"
        }

    private fun getRepositoryNameFromUrl(url: String): String {
        val regex = "github\\.com/[^/]+/([^/]+)".toRegex()
        val matchResult = regex.find(url)

        return matchResult?.groups?.get(1)?.value ?: url
    }
}