package com.flixclusive.domain.provider.manage

import android.content.Context
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.util.awaitFirst
import com.flixclusive.core.datastore.util.rmrf
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withDefaultContext
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.data.provider.ProviderApiRepository
import com.flixclusive.data.provider.ProviderRepository
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.provider.ProviderMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderUnloaderUseCase
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val dataStoreManager: DataStoreManager,
        private val providerRepository: ProviderRepository,
        private val providerApiRepository: ProviderApiRepository,
    ) {
        private val providerPreferences: ProviderPreferences
            get() =
                dataStoreManager
                    .getUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY)
                    .awaitFirst()

        /**
         * Unloads a provider
         *
         * @param metadata the [ProviderMetadata] to uninstall/unload
         * @param unloadOnPreferences an optional toggle to also unload the provider from the settings. Default value is *true*
         */
        suspend fun unload(
            metadata: ProviderMetadata,
            unloadOnPreferences: Boolean = true,
        ) {
            val providerFromPreferences =
                withDefaultContext {
                    providerPreferences
                        .providers
                        .find { it.id == metadata.id }
                }

            requireNotNull(providerFromPreferences) {
                "No such provider on your preferences: ${metadata.name}"
            }

            val provider = providerRepository.getProvider(metadata.id)
            val file = File(providerFromPreferences.filePath)

            if (provider == null || !file.exists()) {
                errorLog("Provider [${metadata.name}] not found. Cannot be unloaded")
                return
            }

            infoLog("Unloading provider: ${provider.name}")
            safeCall("Exception while unloading provider with ID: ${provider.name}") {
                provider.onUnload(context)
            }

            providerRepository.remove(id = metadata.id)
            providerApiRepository.removeApi(id = metadata.id)
            deleteProviderRelatedFiles(file = file)

            if (unloadOnPreferences) {
                providerRepository.removeFromPreferences(id = metadata.id)
            }
        }

        private fun deleteProviderRelatedFiles(file: File) {
            file.delete()

            // Delete updater.json file if its the only thing remaining on that directory
            val parentDirectory = file.parentFile!!
            if (parentDirectory.isDirectory && parentDirectory.listFiles()?.size == 1) {
                val lastRemainingFile = parentDirectory.listFiles()!![0]

                if (lastRemainingFile.name.equals(UPDATER_FILE, true)) {
                    rmrf(parentDirectory)
                }
            }
        }
    }
