package com.flixclusive.domain.provider.util.cache

import com.flixclusive.core.common.locale.UiText
import com.flixclusive.data.provider.repository.CachedLinks
import com.flixclusive.domain.provider.R
import com.flixclusive.core.strings.R as StringsR

internal val EMPTY_PROVIDER_MESSAGE = UiText.from(StringsR.string.no_available_providers)
internal val UNAVAILABLE_EPISODE_MESSAGE = UiText.from(StringsR.string.unavailable_episode)
internal val DEFAULT_ERROR_MESSAGE = UiText.from(StringsR.string.source_data_dialog_state_error_default)

internal fun getNoLinksLoadedMessage(provider: String) =
    UiText.StringResource(R.string.no_links_loaded_format_message, provider)

internal fun CachedLinks.isCached(providerId: String?): Boolean =
    streams.isNotEmpty() &&
        (
            this@isCached.providerId.equals(providerId, true) ||
                providerId == null
        )
