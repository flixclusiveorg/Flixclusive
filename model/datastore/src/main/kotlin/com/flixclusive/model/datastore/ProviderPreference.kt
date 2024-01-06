package com.flixclusive.model.datastore

import kotlinx.serialization.Serializable

/**
 *
 * Data model to use for saving provider list order
 * */
@Serializable
data class ProviderPreference(
    val name: String,
    val isIgnored: Boolean = false,
)