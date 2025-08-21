package com.flixclusive.domain.provider.util

import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.model.provider.ProviderMetadata
import java.io.File

/**
 * Utility object to handle migration of providers to avoid
 * issues with new features included on the core-stubs/gradle module.
 * */
internal object ProviderMigrator {
    /**
     * Checks if the settings file can be migrated to the new format.
     *
     * This is true if the provider's ID in the preferences is empty,
     * which indicates that it was created before the ID-system was introduced.
     *
     * @param metadata The metadata of the provider to check
     * */
    fun ProviderPreferences.canMigrateSettingsFile(metadata: ProviderMetadata): Boolean {
        val providerFromPreference = providers.find {
            it.name.equals(metadata.name, true)
        }

        return providerFromPreference?.id?.isEmpty() == true
    }

    /**
     * Migrates the settings file for a provider from the old format to the new format.
     *
     * This is done by renaming the file from `<name>.json` to `<id>.json`,
     * where `<name>` is the name of the provider and `<id>` is the unique identifier of the provider.
     *
     * @param directory The directory where the provider settings files are stored.
     * @param metadata The metadata of the provider to migrate.
     * */
    fun migrateForOldSettingsFile(
        directory: String,
        metadata: ProviderMetadata,
    ) {
        val providerSettingsDir = File(directory)
        if (!providerSettingsDir.exists()) return

        val files = providerSettingsDir.listFiles() ?: return
        if (files.isEmpty() == true) return

        val oldSettingsFile = File(directory, "${metadata.name}.json")
        if (!files.contains(oldSettingsFile)) return

        val newSettingsFile = File(oldSettingsFile.parentFile, "${metadata.id}.json")
        oldSettingsFile.renameTo(newSettingsFile)
    }
}
