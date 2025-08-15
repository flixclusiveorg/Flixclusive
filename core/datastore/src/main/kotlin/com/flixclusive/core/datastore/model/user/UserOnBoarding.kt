package com.flixclusive.core.datastore.model.user

import kotlinx.serialization.Serializable


@Serializable
data class UserOnBoarding(
    val isFirstTimeOnProvidersScreen: Boolean = true
) : UserPreferences
