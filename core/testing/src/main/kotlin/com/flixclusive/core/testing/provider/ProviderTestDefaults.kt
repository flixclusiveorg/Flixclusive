package com.flixclusive.core.testing.provider

import com.flixclusive.model.provider.Author
import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.ProviderType
import com.flixclusive.model.provider.Repository
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import com.flixclusive.model.provider.Status

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
     * Returns a default [Repository] instance for testing purposes.
     *
     * Default values represent the Flixclusive providers template repository.
     */
    fun getRepository(
        owner: String = "flixclusiveorg",
        name: String = "providers-template",
        url: String = "https://github.com/flixclusiveorg/providers-template",
        rawLinkFormat: String = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/%branch%/%filename%",
    ) = Repository(
        owner = owner,
        name = name,
        url = url,
        rawLinkFormat = rawLinkFormat,
    )

    /**
     * Returns a default [Repository] instance created from a GitHub URL.
     *
     * This is useful for testing the URL parsing functionality.
     */
    fun getRepositoryFromUrl(url: String = "https://github.com/flixclusiveorg/providers-template"): Repository =
        url.toValidRepositoryLink()

    /**
     * Returns a default [ProviderMetadata] instance for testing purposes.
     *
     * Default values represent a working test provider with multiple language support.
     */
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
        status: Status = Status.Working,
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

    /**
     * Returns a default [ProviderMetadata] instance for WebView testing purposes.
     *
     * This variant represents a WebView-based provider.
     */
    fun getWebViewProviderMetadata(
        id: String = "407e8638eb9d50c",
        name: String = "WebView Test Provider",
        authors: List<Author> = listOf(getAuthor()),
        repositoryUrl: String = "https://github.com/flixclusiveorg/providers-template",
        buildUrl: String = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/BasicDummyWebViewProvider.flx",
        changelog: String = DEFAULT_PROVIDER_CHANGELOG,
        versionName: String = "1.0.0",
        versionCode: Long = 10000,
        adult: Boolean = false,
        description: String? = DEFAULT_PROVIDER_DESCRIPTION,
        iconUrl: String? = null,
        language: Language = Language.Multiple,
        providerType: ProviderType = ProviderType.All,
        status: Status = Status.Working,
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

    /**
     * Returns a list of default [ProviderMetadata] instances for testing purposes.
     *
     * Useful for testing scenarios that require multiple providers.
     */
    fun getProviderMetadataList(): List<ProviderMetadata> =
        listOf(
            getProviderMetadata(),
            getWebViewProviderMetadata(),
        )
}
