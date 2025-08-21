package com.flixclusive.domain.provider.usecase.manage.impl

import android.content.Context
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.datastore.util.rmrf
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import com.flixclusive.domain.provider.util.Constants
import com.flixclusive.model.provider.ProviderMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject

internal class UnloadProviderUseCaseImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val dataStoreManager: DataStoreManager,
        private val providerRepository: ProviderRepository,
        private val providerApiRepository: ProviderApiRepository,
    ) : UnloadProviderUseCase {
        private suspend fun getProviderPrefs() =
            dataStoreManager
                .getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
                .first()

        override suspend operator fun invoke(
            metadata: ProviderMetadata,
            unloadFromPrefs: Boolean,
        ): Boolean {
            val providers = getProviderPrefs().providers
            val providerFromPreferences = providers.find { it.id == metadata.id }

            requireNotNull(providerFromPreferences) {
                "No such provider on your preferences: ${metadata.name}"
            }

            val provider = providerRepository.getProvider(metadata.id)
            val file = File(providerFromPreferences.filePath)

            if (provider == null || !file.exists()) {
                errorLog("Provider [${metadata.name}] not found. Cannot be unloaded")
                return false
            }

            infoLog("Unloading provider: ${provider.name}")
            safeCall("Exception while unloading provider with ID: ${provider.name}") {
                provider.onUnload(context)
            }

            providerRepository.remove(id = metadata.id)
            providerApiRepository.removeApi(id = metadata.id)
            deleteProviderRelatedFiles(file = file)

            if (unloadFromPrefs) {
                providerRepository.removeFromPreferences(id = metadata.id)
            }

            return true
        }

        private fun deleteProviderRelatedFiles(file: File) {
            file.delete()

            // Delete updater.json file if its the only thing remaining on that directory
            val parentDirectory = file.parentFile!!
            if (parentDirectory.isDirectory && parentDirectory.listFiles()?.size == 1) {
                val lastRemainingFile = parentDirectory.listFiles()!![0]

                if (lastRemainingFile.name.equals(Constants.UPDATER_FILE, true)) {
                    rmrf(parentDirectory)
                }
            }
        }
    }
