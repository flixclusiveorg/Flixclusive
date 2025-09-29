package com.flixclusive.data.app.updates.repository

import com.flixclusive.data.app.updates.model.AppUpdateInfo

interface AppUpdatesRepository {
    suspend fun getLatestUpdate(): Result<AppUpdateInfo?>
}
