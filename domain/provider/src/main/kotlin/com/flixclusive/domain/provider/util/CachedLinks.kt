package com.flixclusive.domain.provider.util

import com.flixclusive.core.locale.UiText
import com.flixclusive.domain.provider.CachedLinks
import com.flixclusive.core.locale.R as LocaleR

internal val EMPTY_PROVIDER_MESSAGE = UiText.StringResource(LocaleR.string.no_available_providers)
internal val UNAVAILABLE_EPISODE_MESSAGE = UiText.StringResource(LocaleR.string.unavailable_episode)
internal val DEFAULT_ERROR_MESSAGE = UiText.StringResource(LocaleR.string.source_data_dialog_state_error_default)

internal fun getNoLinksLoadedMessage(provider: String) =
    UiText.StringResource(LocaleR.string.no_links_loaded_format_message, provider)

internal fun CachedLinks.isCached(providerId: String?): Boolean =
    streams.isNotEmpty() &&
        (
            this@isCached.providerId.equals(providerId, true) ||
                providerId == null
        )
