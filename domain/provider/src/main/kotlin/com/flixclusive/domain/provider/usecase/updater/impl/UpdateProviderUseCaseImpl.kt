package com.flixclusive.domain.provider.usecase.updater.impl

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.ProviderFromPreferences
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.get.GetProviderFromRemoteUseCase
import com.flixclusive.domain.provider.usecase.manage.LoadProviderUseCase
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import com.flixclusive.domain.provider.usecase.updater.ProviderUpdateResult
import com.flixclusive.domain.provider.usecase.updater.UpdateProviderUseCase
import com.flixclusive.domain.provider.util.extensions.createFileForProvider
import com.flixclusive.domain.provider.util.extensions.downloadProvider
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject

private const val CHANNEL_UPDATER_ID = "PROVIDER_UPDATER_CHANNEL_ID"
private const val CHANNEL_UPDATER_NAME = "PROVIDER_UPDATER_CHANNEL"

// TODO: Remove the usecase dependencies from the implementation.
//       It's a pain in the ass to test this class
internal class UpdateProviderUseCaseImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val dataStoreManager: DataStoreManager,
        private val userSessionDataStore: UserSessionDataStore,
        private val providerRepository: ProviderRepository,
        private val loadProviderUseCase: LoadProviderUseCase,
        private val unloadProviderUseCase: UnloadProviderUseCase,
        private val getProviderFromRemoteUseCase: GetProviderFromRemoteUseCase,
        private val client: OkHttpClient,
        private val appDispatchers: AppDispatchers,
    ) : UpdateProviderUseCase {
        private suspend fun getProviderPrefs() =
            dataStoreManager
                .getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
                .first()

        @Throws(Throwable::class)
        override suspend fun invoke(provider: ProviderMetadata): Boolean {
            requireNotNull(providerRepository.getProvider(provider.id)) {
                "No such provider: ${provider.name}"
            }

            val repository = provider.repositoryUrl.toValidRepositoryLink()
            val updatedMetadata = getProviderFromRemoteUseCase(
                repository = repository,
                id = provider.id,
            ).getOrThrow()

            val newPreference = getUpdatedPreferenceItem(
                id = provider.id,
                newMetadata = updatedMetadata,
            )

            providerRepository.addToPreferences(preferenceItem = newPreference)

            withContext(appDispatchers.io) {
                client.downloadProvider(
                    saveTo = File(newPreference.filePath),
                    buildUrl = updatedMetadata.buildUrl,
                )
            }

            unloadProviderUseCase(
                metadata = provider,
                unloadFromPrefs = false,
            )

            loadProviderUseCase(
                metadata = updatedMetadata,
                filePath = newPreference.filePath,
            )

            return true
        }

        override suspend fun invoke(providers: List<ProviderMetadata>): ProviderUpdateResult {
            val updatedProviders = mutableListOf<ProviderMetadata>()
            val failedToUpdateProviders = mutableListOf<Pair<ProviderMetadata, Throwable>>()

            for (provider in providers) {
                try {
                    if (invoke(provider)) {
                        updatedProviders.add(provider)
                    }
                } catch (e: Throwable) {
                    errorLog(e)
                    failedToUpdateProviders.add(provider to e)
                }
            }

            return ProviderUpdateResult(
                success = updatedProviders,
                failed = failedToUpdateProviders,
            )
        }

        private suspend fun getUpdatedPreferenceItem(
            id: String,
            newMetadata: ProviderMetadata,
        ): ProviderFromPreferences {
            val providers = getProviderPrefs().providers
            val oldOrderPosition = providers.indexOfFirst { it.id == id }

            val oldPreference = providers[oldOrderPosition]

            val userId = userSessionDataStore.currentUserId.filterNotNull().first()

            val file = context.createFileForProvider(
                userId = userId,
                provider = newMetadata,
            )

            return oldPreference.copy(
                name = newMetadata.name,
                filePath = file.absolutePath,
            )
        }
    }
