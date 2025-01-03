package com.flixclusive.core.datastore.migration.model

import kotlinx.serialization.Serializable


@Serializable
internal data class OldOnBoardingPreferences(
    val isFirstTimeUserLaunch_: Boolean = true, // TODO: Remove underscore after implementing better on-boarding screen
    val lastSeenChangelogsVersion: Long = -1L,
    val isFirstTimeOnProvidersScreen: Boolean = true,
)