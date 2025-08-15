package com.flixclusive.core.datastore.model.user

import kotlinx.serialization.Serializable

@Serializable
data class UiPreferences(
    val shouldShowTitleOnCards: Boolean = false,
) : UserPreferences
