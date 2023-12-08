package com.flixclusive.domain.preferences

import kotlinx.serialization.Serializable

@Serializable
data class ProviderConfiguration(
    val name: String,
    val isIgnored: Boolean = false,
)