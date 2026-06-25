package com.flixclusive.core.testing.provider

import com.flixclusive.model.provider.Author
import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.ProviderStatus
import com.flixclusive.model.provider.ProviderType

/**
 * Provides default values for testing provider-related functionality.
 *
 * This is useful for creating mock data in tests without needing to define
 * repetitive test data setup. All methods provide sensible defaults while
 * allowing customization through parameters.
 */
object ProviderTestDefaults {
    const val DEFAULT_PROVIDER_DESCRIPTION = "A dummy provider that does nothing."
    const val DEFAULT_PROVIDER_CHANGELOG = "# Header\n## Secondary header\n---\n\nList\n- Item 1\n- Item 2\n- Item 3"

    /**
     * Returns a default [Author] instance for testing purposes.
     *
     * Default values represent a typical provider author from the Flixclusive organization.
     */
    fun getAuthor(
        name: String = "flixclusiveorg",
        image: String? = "http://github.com/flixclusiveorg.png",
        socialLink: String? = "http://github.com/flixclusiveorg",
    ) = Author(
        name = name,
        image = image,
        socialLink = socialLink,
    )

    /**
     * Returns a default [ProviderMetadata] instance for testing purposes.
     *
     * Default values represent a working test provider with multiple language support.
     */
    @Suppress("ktlint:standard:max-line-length")
    fun getProviderMetadata(
        id: String = "14a5037ac9553dd",
        name: String = "Test Provider",
        authors: List<Author> = listOf(getAuthor()),
        repositoryUrl: String = "https://github.com/flixclusiveorg/providers-template",
        buildUrl: String = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/BasicDummyProvider.flx",
        changelog: String = DEFAULT_PROVIDER_CHANGELOG,
        versionName: String = "1.0.0",
        versionCode: Long = 10000,
        adult: Boolean = false,
        description: String? = DEFAULT_PROVIDER_DESCRIPTION,
        iconUrl: String? = null,
        language: Language = Language.Multiple,
        providerType: ProviderType = ProviderType.All,
        status: ProviderStatus = ProviderStatus.Working,
    ) = ProviderMetadata(
        id = id,
        name = name,
        authors = authors,
        repositoryUrl = repositoryUrl,
        buildUrl = buildUrl,
        changelog = changelog,
        versionName = versionName,
        versionCode = versionCode,
        adult = adult,
        description = description,
        iconUrl = iconUrl,
        language = language,
        providerType = providerType,
        status = status,
    )
}
