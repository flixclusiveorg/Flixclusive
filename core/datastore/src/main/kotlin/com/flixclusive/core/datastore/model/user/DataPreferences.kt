package com.flixclusive.core.datastore.model.user

import kotlinx.serialization.Serializable

@Serializable
data class DataPreferences(
    val isIncognito: Boolean = false,
) : UserPreferences
