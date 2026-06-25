package com.flixclusive.core.testing.database

import android.content.Context
import androidx.room.Room
import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.core.database.entity.provider.InstalledRepository
import com.flixclusive.core.database.entity.user.User

/**
 * Test defaults for database-related tests.
 * */
object DatabaseTestDefaults {
    const val TEST_USER_ID = "11111111-1111-1111-1111-111111111111"

    fun getUser(
        id: String = TEST_USER_ID,
        name: String = "Test User",
        image: Int = 1,
        pin: String? = null,
        pinHint: String? = null,
    ) = User(
        id = id,
        name = name,
        image = image,
        pin = pin,
        pinHint = pinHint,
    )

    fun getInstalledRepository(
        url: String = "https://example.com/repo",
        owner: String = "testowner",
        name: String = "testrepo",
        userId: String = TEST_USER_ID,
        rawLinkFormat: String = "https://raw.example.com/%s",
    ) = InstalledRepository(
        url = url,
        owner = owner,
        name = name,
        rawLinkFormat = rawLinkFormat,
        userId = userId,
    )

    fun getInstalledProvider(
        id: String = "test-provider",
        repositoryUrl: String = "https://example.com/repo",
        filePath: String = "provider.json",
        ownerId: String = TEST_USER_ID,
        isDebug: Boolean = false,
        isCatalogEnabled: Boolean = true,
        isCrossMatchEnabled: Boolean = true,
        isMediaLinkEnabled: Boolean = true,
        isMetadataEnabled: Boolean = true,
        isSearchEnabled: Boolean = true,
        isTrackerEnabled: Boolean = true,
    ) = InstalledProvider(
        id = id,
        repositoryUrl = repositoryUrl,
        ownerId = ownerId,
        filePath = filePath,
        isDebug = isDebug,
        isCatalogEnabled = isCatalogEnabled,
        isCrossMatchEnabled = isCrossMatchEnabled,
        isMediaLinkEnabled = isMediaLinkEnabled,
        isMetadataEnabled = isMetadataEnabled,
        isSearchEnabled = isSearchEnabled,
        isTrackerEnabled = isTrackerEnabled,
    )

    fun createDatabase(context: Context) =
        Room
            .inMemoryDatabaseBuilder(
                context = context,
                klass = AppDatabase::class.java,
            ).allowMainThreadQueries()
            .build()
}
