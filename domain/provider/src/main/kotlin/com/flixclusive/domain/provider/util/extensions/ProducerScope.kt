package com.flixclusive.domain.provider.util.extensions

import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.domain.provider.R
import kotlinx.coroutines.channels.ProducerScope

/**
 * Sends a message on the [ProducerScope] indicating that the episode is being fetched.
 * */
fun ProducerScope<LoadLinksState>.sendFetchingEpisodeMessage() =
    trySend(LoadLinksState.Fetching(UiText.StringResource(R.string.fetching_episode_message)))

/**
 * Sends a message on the [ProducerScope] indicating that the film is being fetched from a specific provider.
 * */
fun ProducerScope<LoadLinksState>.sendFetchingFilmMessage(provider: String) =
    trySend(
        LoadLinksState.Fetching(
            UiText.StringResource(
                R.string.fetching_from_provider_format,
                provider,
            ),
        ),
    )

/**
 * Sends a message on the [ProducerScope] indicating that links are being extracted from a specific provider.
 *
 * @param provider the name of the provider from which links are being extracted
 * @param isOnWebView indicates whether the extraction is happening on a web view
 * */
fun ProducerScope<LoadLinksState>.sendExtractingLinksMessage(
    provider: String,
    isOnWebView: Boolean = false,
) {
    val messageFormat =
        if (isOnWebView) {
            R.string.extracting_from_web_view_provider_format
        } else {
            R.string.extracting_from_provider_format
        }

    trySend(
        LoadLinksState.Extracting(
            UiText.StringResource(messageFormat, provider),
        ),
    )
}
