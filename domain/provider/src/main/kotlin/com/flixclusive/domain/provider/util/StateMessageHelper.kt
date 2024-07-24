package com.flixclusive.domain.provider.util

import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.model.provider.MediaLinkResourceState
import kotlinx.coroutines.channels.ProducerScope
import com.flixclusive.core.util.R as UtilR

internal object StateMessageHelper {
    fun ProducerScope<MediaLinkResourceState>.sendFetchingEpisodeMessage() =
        trySend(MediaLinkResourceState.Fetching(UiText.StringResource(UtilR.string.fetching_episode_message)))

    fun ProducerScope<MediaLinkResourceState>.sendFetchingFilmMessage(
        provider: String
    ) = trySend(
        MediaLinkResourceState.Fetching(
            UiText.StringResource(
                UtilR.string.fetching_from_provider_format,
                provider
            )
        )
    )

    fun ProducerScope<MediaLinkResourceState>.sendExtractingLinksMessage(
        provider: String,
        isOnWebView: Boolean = false
    ) {
        val messageFormat = if (isOnWebView) {
            UtilR.string.extracting_from_web_view_provider_format
        } else UtilR.string.extracting_from_provider_format

        trySend(
            MediaLinkResourceState.Extracting(
                UiText.StringResource(messageFormat, provider)
            )
        )
    }

    fun ProducerScope<MediaLinkResourceState>.throwError(error: UiText?) =
        trySend(MediaLinkResourceState.Error(error))

    fun ProducerScope<MediaLinkResourceState>.throwError(error: Throwable?) =
        trySend(MediaLinkResourceState.Error(error))

    fun ProducerScope<MediaLinkResourceState>.throwUnavailableError(error: UiText?) =
        trySend(MediaLinkResourceState.Error(error))

    fun ProducerScope<MediaLinkResourceState>.finish() = trySend(MediaLinkResourceState.Success)
}