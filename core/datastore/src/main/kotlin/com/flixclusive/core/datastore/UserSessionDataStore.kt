package com.flixclusive.core.datastore

import kotlinx.coroutines.flow.Flow

interface UserSessionDataStore {
    val currentUserId: Flow<String?>

    // TODO: Remove legacyCurrentUserId in future versions after migration is complete
    @Deprecated("This field is only used for migration purposes and will be removed in future versions")
    val legacyCurrentUserId: Flow<Int?>
    val sessionTimeout: Flow<Long>

    suspend fun saveCurrentUserId(
        userId: String,
        // TODO: Remove legacyUserId in future versions after migration is complete
        legacyUserId: Int
    )

    suspend fun clearCurrentUser()
}
