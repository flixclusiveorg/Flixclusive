package com.flixclusive.model.datastore.user

import kotlinx.serialization.Serializable

@Serializable
data class DataPreferences(
    val isIncognito: Boolean = false,
) : UserPreferences