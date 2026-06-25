package com.flixclusive.domain.provider.util.extensions

import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.domain.provider.R
import com.flixclusive.model.provider.ProviderMetadata
import kotlinx.coroutines.channels.ProducerScope

/**
 * Sends a message on the [ProducerScope] indicating that links are being extracted from a specific provider.
 *
 * @param provider the name of the provider from which links are being extracted
 * */
fun ProducerScope<LoadLinksState>.sendExtractingLinksMessage(provider: ProviderMetadata) {
    trySend(
        LoadLinksState.Extracting(
            providerId = provider.id,
            message = UiText.StringResource(R.string.get_media_links_extracting_from_provider, provider.name),
        ),
    )
}

fun ProducerScope<LoadLinksState>.sendCrossMatchingMessage(provider: ProviderMetadata) {
    trySend(
        LoadLinksState.Extracting(
            providerId = provider.id,
            message = UiText.from(
                R.string.get_media_links_cross_matching_provider,
                provider.name,
            ),
        ),
    )
}
