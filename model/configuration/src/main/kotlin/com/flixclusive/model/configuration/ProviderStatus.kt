package com.flixclusive.model.configuration

/**
 *
 * Required for the response of [FlixclusiveConfigService]
 *
 * */
data class ProviderStatus(
    val name: String,
    val isMaintenance: Boolean
)