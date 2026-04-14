package com.flixclusive.core.datastore

import androidx.datastore.preferences.core.Preferences
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

interface DataStoreManager {
    fun getSystemPrefs(): Flow<SystemPreferences>

    fun usePreferencesByUserId(
        userId: String,
        legacyUserId: Int? = null, // TODO: Remove legacyUserId in future versions after migration is complete
    )

    suspend fun updateSystemPrefs(transform: suspend (t: SystemPreferences) -> SystemPreferences)

    fun <T : UserPreferences> getUserPrefs(
        key: Preferences.Key<String>,
        type: KClass<T>,
    ): Flow<T>

    suspend fun <T : UserPreferences> updateUserPrefs(
        key: Preferences.Key<String>,
        type: KClass<T>,
        transform: suspend (T) -> T,
    )

    suspend fun deleteAllUserRelatedFiles(userId: String)
}
