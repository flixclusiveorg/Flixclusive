package com.flixclusive.model.datastore.user

import kotlinx.serialization.Serializable


@Serializable
data class UserOnBoarding(
    val isFirstTimeOnProvidersScreen: Boolean = true
) : UserPreferences