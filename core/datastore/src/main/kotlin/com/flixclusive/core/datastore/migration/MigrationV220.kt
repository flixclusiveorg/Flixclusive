package com.flixclusive.core.datastore.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.core.database.dao.provider.InstalledProviderDao
import com.flixclusive.core.database.dao.provider.InstalledRepositoryDao
import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.core.database.entity.provider.InstalledRepository
import com.flixclusive.core.datastore.migration.model.OldProviderFromPreferences
import com.flixclusive.core.datastore.migration.model.ProviderPreferencesV213
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.model.provider.ProviderMetadata
import kotlinx.serialization.json.Json
import java.io.File

@Suppress("DEPRECATION")
internal class MigrationV220(
    private val userId: Int,
    private val providerDao: InstalledProviderDao,
    private val repositoryDao: InstalledRepositoryDao,
) : DataMigration<Preferences> {
    override suspend fun cleanUp() {
        // No - op, since we are not deleting any old data or files in this migration
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val mutablePrefs = currentData.toMutablePreferences()

        val providerData = mutablePrefs[UserPreferences.PROVIDER_PREFS_KEY] ?: return currentData
        val oldProviderPrefs = Json.decodeFromString<ProviderPreferencesV213>(providerData)
        migrateProvidersToDatabase(oldProviderPrefs)
        migrateRepositoriesToDatabase(oldProviderPrefs)

        mutablePrefs[UserPreferences.PROVIDER_PREFS_KEY] = Json.encodeToString(
            ProviderPreferences(
                shouldWarnBeforeInstall = oldProviderPrefs.shouldWarnBeforeInstall,
                isAutoUpdateEnabled = oldProviderPrefs.isAutoUpdateEnabled,
                shouldAddDebugPrefix = oldProviderPrefs.shouldAddDebugPrefix,
                providers = oldProviderPrefs.providers
            )
        )

        return mutablePrefs.toPreferences()
    }

    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val data = currentData[UserPreferences.PROVIDER_PREFS_KEY] ?: return false
        val providerSettings = Json.decodeFromString<ProviderPreferencesV213>(data)

        return providerSettings.providers.isNotEmpty()
            || providerSettings.repositories.isNotEmpty()
    }

    private suspend fun migrateRepositoriesToDatabase(
        oldProviderPrefs: ProviderPreferencesV213
    ) {
        val dbRepositories = oldProviderPrefs.repositories.map { repository ->
            InstalledRepository(
                userId = userId,
                url = repository.url,
                owner = repository.owner,
                name = repository.name,
                rawLinkFormat = repository.rawLinkFormat,
            )
        }

        repositoryDao.insert(dbRepositories)
    }

    private suspend fun migrateProvidersToDatabase(
        oldProviderPrefs: ProviderPreferencesV213
    ) {
        val metadataList = oldProviderPrefs.providers.mapNotNull { preference ->
            val metadata = findProviderFromUpdaterJson(preference.id, preference.filePath)
            if (metadata == null) {
                warnLog("Warning: Could not find metadata for provider with id ${preference.id} and file path ${preference.filePath}. Skipping this provider...")
            }

            metadata
        }

        val dbProviders = metadataList.mapIndexedNotNull { index, metadata ->
            val correspondingPreference = oldProviderPrefs.providers.firstOrNull { it.id == metadata.id }
            if (correspondingPreference == null) {
                warnLog("Warning: Could not find corresponding preference for provider with id ${metadata.id}. Skipping this provider...")
                return@mapIndexedNotNull null
            }

            convertToDbProvider(index, correspondingPreference, metadata)
        }

        providerDao.insert(dbProviders)
    }

    private fun convertToDbProvider(
        index: Int,
        preference: OldProviderFromPreferences,
        metadata: ProviderMetadata
    ): InstalledProvider {
        return InstalledProvider(
            id = preference.id,
            repositoryUrl = metadata.repositoryUrl,
            isEnabled = !preference.isDisabled,
            sortOrder = index.toDouble(),
            isDebug = preference.isDebug,
            filePath = preference.filePath,
            ownerId = userId
        )
    }

    private fun findProviderFromUpdaterJson(
        providerId: String,
        filePath: String
    ): ProviderMetadata? {
        val providerFile = File(filePath)
        val parentFolder = providerFile.parentFile ?: return null
        val updaterJsonFile = File(parentFolder, "updater.json")
        if (!updaterJsonFile.exists()) return null

        val updaterJson = fromJson<List<ProviderMetadata>>(updaterJsonFile.reader())
        return updaterJson.firstOrNull { it.id == providerId }
    }
}
