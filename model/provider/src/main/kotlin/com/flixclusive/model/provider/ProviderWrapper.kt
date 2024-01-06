package com.flixclusive.model.provider

import com.flixclusive.provider.base.Provider


/**
 *
 * Wrapper class to be used for preferences.
 *
 * This is a useless model and will be removed soon
 * as soon as I implemented the plugins system of this app.
 *
 * */
data class ProviderWrapper(
    val provider: Provider,
    val isMaintenance: Boolean = false,
    val isIgnored: Boolean = false,
)