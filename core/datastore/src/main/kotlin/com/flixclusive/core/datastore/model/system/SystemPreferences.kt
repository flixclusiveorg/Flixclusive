package com.flixclusive.core.datastore.model.system

import com.flixclusive.core.datastore.model.FlixclusivePrefs
import com.flixclusive.core.datastore.model.user.network.DoHPreference
import com.flixclusive.core.util.network.okhttp.USER_AGENT
import kotlinx.serialization.Serializable

@Serializable
data class SystemPreferences(
    val isFirstTimeUserLaunch: Boolean = true,
    val lastSeenChangelogs: Long = -1L,
    val isUsingAutoUpdateAppFeature: Boolean = true,
    val isUsingPrereleaseUpdates: Boolean = false,
    val isSendingCrashLogsAutomatically: Boolean = true,
    val dns: DoHPreference = DoHPreference.None,
    val userAgent: String = USER_AGENT,
) : FlixclusivePrefs
