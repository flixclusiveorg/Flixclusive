package com.flixclusive.model.datastore

import kotlinx.serialization.Serializable

/**
 *
 * Data model to use for saving
 * plugin list order and plugin usability
 * */
@Serializable
data class PluginPreference(
    val name: String,
    val filePath: String,
    val isDisabled: Boolean = false,
)