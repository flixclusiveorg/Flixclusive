package com.flixclusive.domain.provider.usecase.manage.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.provider.ProviderConstants
import com.flixclusive.core.common.provider.ProviderFile
import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.data.provider.repository.InstalledRepoRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.manage.InitializeProvidersUseCase
import com.flixclusive.domain.provider.usecase.manage.LoadProviderUseCase
import com.flixclusive.domain.provider.util.extensions.toInstalledRepository
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import javax.inject.Inject

// TODO: Add a mapping or list of providers that have failed to initialize
//       and provide a way to retry initialization for those providers.
//       This will be useful to show a notification or a dialog to the user
//       to retry initialization of those providers.
internal class InitializeProvidersUseCaseImpl @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val userSessionDataStore: UserSessionDataStore,
    private val loadProviderUseCase: LoadProviderUseCase,
    private val providerRepository: ProviderRepository,
    private val installedRepoRepository: InstalledRepoRepository,
    private val appDispatchers: AppDispatchers,
) : InitializeProvidersUseCase {
    private val mutex = Mutex()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    override fun invoke(skipLoading: Boolean) = channelFlow {
        if (mutex.isLocked) {
            warnLog("Provider initialization is already in progress. Skipping initialization...")
            return@channelFlow
        }

        withContext(appDispatchers.io) {
            mutex.withLock {
                val userId = userSessionDataStore.currentUserId.filterNotNull().first()

                initializeDebugProviders(userId)
                val providers = providerRepository.getProviders(userId)

                providers.forEach { providerWrapper ->
                    loadProviderUseCase(installedProvider = providerWrapper.provider)
                        .collect(::send)
                }

                _isLoading.update { false }
            }
        }
    }.onStart {
        _isLoading.update { !skipLoading }
    }.onCompletion {
        if (it != null) {
            errorLog("Failed to initialize providers")
            errorLog(it)
        }
    }

    /**
     * Initializes all debug providers from local storage and adds
     * them to the preferences, if they are not already present.
     * */
    private suspend fun initializeDebugProviders(userId: String) {
        val path = ProviderFile.getDebugProvidersDirPath()
        val localDir = File(path)

        if (!localDir.exists()) {
            localDir.mkdirs()
            return
        }

        val repositoryDirectory = localDir.listFiles()

        val addDebugSuffix = dataStoreManager
            .getUserPrefsAsFlow(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
            .map { it.shouldAddDebugPrefix }
            .first()

        repositoryDirectory?.forEach { subDirectory ->
            if (!subDirectory.isDirectory) return@forEach

            val updaterFile = File(subDirectory.absolutePath + "/${ProviderConstants.UPDATER_JSON_FILE}")
            if (!updaterFile.exists()) {
                warnLog("Provider's `updater.json` could not be found!")
                return@forEach
            }

            val updaterJson = fromJson<List<ProviderMetadata>>(updaterFile.reader())
            val repository = updaterJson
                .firstOrNull()
                ?.repositoryUrl
                ?.toValidRepositoryLink()
                ?: return@forEach

            val isRepositoryInstalled = installedRepoRepository.isInstalled(
                url = repository.url,
                ownerId = userId
            )
            if (!isRepositoryInstalled) {
                installedRepoRepository.insert(
                    repository.toInstalledRepository(userId)
                )
            }

            val subFiles = subDirectory.listFiles()
            subFiles?.forEach subDirectory@{ providerFile ->
                if (providerFile.name.equals(ProviderConstants.UPDATER_JSON_FILE, true)) {
                    return@subDirectory
                }

                val metadata = updaterJson
                    .find {
                        it.buildUrl.endsWith(providerFile.name)
                    }?.let {
                        if (!addDebugSuffix) {
                            return@let it
                        }

                        it.copy(
                            id = "${it.id}${ProviderPreferences.DEBUG_SUFFIX}",
                            name = "${it.name}${ProviderPreferences.DEBUG_SUFFIX}",
                        )
                    }

                if (metadata == null) {
                    warnLog("No metadata found for provider file: ${providerFile.name}")
                    return@subDirectory
                }

                val installedProvider = providerRepository
                    .getProvider(
                        ownerId = userId,
                        id = metadata.id,
                    )?.provider

                infoLog("New debug provider found: ${metadata.name}. Installing...")
                providerRepository.install(
                    metadata = metadata,
                    provider = installedProvider?.copy(
                        filePath = providerFile.absolutePath,
                        isDebug = true,
                        updatedAt = Date()
                    ) ?: InstalledProvider(
                        ownerId = userId,
                        id = metadata.id,
                        repositoryUrl = metadata.repositoryUrl,
                        filePath = providerFile.absolutePath,
                        isDebug = true
                    ),
                )
            }
        }
    }
}
