package com.flixclusive.domain.provider.util.extensions

import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.MediaLinkResourceState
import com.flixclusive.domain.provider.R
import kotlinx.coroutines.channels.ProducerScope

/**
 * Sends a message on the [ProducerScope] indicating that the episode is being fetched.
 * */
fun ProducerScope<MediaLinkResourceState>.sendFetchingEpisodeMessage() =
    trySend(MediaLinkResourceState.Fetching(UiText.StringResource(R.string.fetching_episode_message)))

/**
 * Sends a message on the [ProducerScope] indicating that the film is being fetched from a specific provider.
 * */
fun ProducerScope<MediaLinkResourceState>.sendFetchingFilmMessage(provider: String) =
    trySend(
        MediaLinkResourceState.Fetching(
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
fun ProducerScope<MediaLinkResourceState>.sendExtractingLinksMessage(
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
        MediaLinkResourceState.Extracting(
            UiText.StringResource(messageFormat, provider),
        ),
    )
}
