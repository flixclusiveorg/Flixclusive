package com.flixclusive.domain.provider.util

import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.model.provider.CachedLinks
import com.flixclusive.core.util.R as UtilR

internal object MediaLinksProviderUtil {
    val EMPTY_PROVIDER_MESSAGE = UiText.StringResource(UtilR.string.no_available_providers)
    val UNAVAILABLE_EPISODE_MESSAGE = UiText.StringResource(UtilR.string.unavailable_episode)
    val DEFAULT_ERROR_MESSAGE = UiText.StringResource(UtilR.string.source_data_dialog_state_error_default)
    const val DEFAULT_WEB_VIEW_ERROR_MESSAGE = "WEBVIEW UNKNOWN ERROR"

    fun getNoLinksLoadedMessage(provider: String)
        = UiText.StringResource(UtilR.string.no_links_loaded_format_message, provider)

    fun CachedLinks.isCached(providerName: String?): Boolean
         = streams.isNotEmpty()
            && (providerName.equals(providerName, true)
            || providerName == null)
}