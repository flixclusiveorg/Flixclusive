package com.flixclusive.domain.provider.util

import com.flixclusive.core.locale.UiText
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState
import kotlinx.coroutines.channels.ProducerScope
import com.flixclusive.core.locale.R as LocaleR

internal object GetMediaLinksStateMessageHelper {
    fun ProducerScope<MediaLinkResourceState>.sendFetchingEpisodeMessage() =
        trySend(MediaLinkResourceState.Fetching(UiText.StringResource(LocaleR.string.fetching_episode_message)))

    fun ProducerScope<MediaLinkResourceState>.sendFetchingFilmMessage(
        provider: String
    ) = trySend(
        MediaLinkResourceState.Fetching(
            UiText.StringResource(
                LocaleR.string.fetching_from_provider_format,
                provider
            )
        )
    )

    fun ProducerScope<MediaLinkResourceState>.sendExtractingLinksMessage(
        provider: String,
        isOnWebView: Boolean = false
    ) {
        val messageFormat = if (isOnWebView) {
            LocaleR.string.extracting_from_web_view_provider_format
        } else LocaleR.string.extracting_from_provider_format

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
        trySend(MediaLinkResourceState.Unavailable(error))

    fun ProducerScope<MediaLinkResourceState>.finish() = trySend(MediaLinkResourceState.Success)

    fun ProducerScope<MediaLinkResourceState>.finishWithTrustedProviders() = trySend(MediaLinkResourceState.SuccessWithTrustedProviders)
}