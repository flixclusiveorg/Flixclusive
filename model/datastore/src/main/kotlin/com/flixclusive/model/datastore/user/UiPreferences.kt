package com.flixclusive.model.datastore.user

import kotlinx.serialization.Serializable

@Serializable
data class UiPreferences(
    val shouldShowTitleOnCards: Boolean = false,
) : UserPreferences
