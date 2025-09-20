package com.flixclusive.core.common.provider

import com.flixclusive.model.provider.ProviderMetadata

/**
 * A data class that holds a [ProviderMetadata] and a [Throwable].
 *
 * This is useful for returning both a provider and an error that occurred while processing it.
 *
 * @property provider The provider metadata.
 * @property throwable The error that occurred.
 * */
data class ProviderWithThrowable(
    val provider: ProviderMetadata,
    val throwable: Throwable
)
