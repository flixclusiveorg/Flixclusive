package com.flixclusive.domain.model.provider

import com.flixclusive.providers.interfaces.SourceProvider

data class SourceProviderDetails(
    val provider: SourceProvider,
    val isMaintenance: Boolean = false,
    val isIgnored: Boolean = false,
)