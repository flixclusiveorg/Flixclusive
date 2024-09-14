package com.flixclusive.domain.provider.util

import com.flixclusive.core.locale.UiText
import com.flixclusive.domain.provider.CachedLinks
import com.flixclusive.model.provider.link.MediaLink
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import com.flixclusive.core.locale.R as LocaleR

internal object MediaLinksProviderUtil {
    val EMPTY_PROVIDER_MESSAGE = UiText.StringResource(LocaleR.string.no_available_providers)
    val UNAVAILABLE_EPISODE_MESSAGE = UiText.StringResource(LocaleR.string.unavailable_episode)
    val DEFAULT_ERROR_MESSAGE = UiText.StringResource(LocaleR.string.source_data_dialog_state_error_default)

    fun getNoLinksLoadedMessage(provider: String)
        = UiText.StringResource(LocaleR.string.no_links_loaded_format_message, provider)

    fun CachedLinks.isCached(providerName: String?): Boolean
         = streams.isNotEmpty()
            && (providerName.equals(providerName, true)
            || providerName == null)

    fun MediaLink.isValidLink(): Boolean {
        return url.toHttpUrlOrNull() != null
    }
}