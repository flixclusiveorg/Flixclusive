package com.flixclusive.model.datastore

import kotlinx.serialization.Serializable


@Serializable
data class OnBoardingPreferences(
    val isFirstTimeUserLaunch_: Boolean = true, // TODO: Remove underscore after implementing better on-boarding screen
    val lastSeenChangelogsVersion: Long = -1L,
    val isFirstTimeOnProvidersScreen: Boolean = true,
)