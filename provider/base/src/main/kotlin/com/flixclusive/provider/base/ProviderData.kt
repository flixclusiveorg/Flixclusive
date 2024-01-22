package com.flixclusive.provider.base


/**
 *
 * Wrapper class to be used for preferences.
 *
 * This is a useless model and will be removed soon
 * as soon as I implemented the plugins system of this app.
 *
 * */
data class ProviderData(
    val provider: Provider,
    val isMaintenance: Boolean = false,
    val isIgnored: Boolean = false,
)