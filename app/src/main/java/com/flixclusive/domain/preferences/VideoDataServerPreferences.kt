package com.flixclusive.domain.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

interface VideoDataServerPreferences {
    val Context.dataStore: DataStore<Preferences>
    val userPreferredServerKey: Preferences.Key<String>

    val getPreferredServer: Flow<String?>

    suspend fun savePreferredServer(serverName: String)
}