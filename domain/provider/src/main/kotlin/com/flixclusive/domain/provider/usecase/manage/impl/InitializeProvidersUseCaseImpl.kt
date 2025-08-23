package com.flixclusive.domain.provider.usecase.manage.impl

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.PROVIDERS_FOLDER_NAME
import com.flixclusive.core.datastore.model.user.ProviderFromPreferences
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.datastore.util.rmrf
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.domain.provider.usecase.manage.InitializeProvidersUseCase
import com.flixclusive.domain.provider.usecase.manage.LoadProviderUseCase
import com.flixclusive.domain.provider.util.Constants
import com.flixclusive.domain.provider.util.extensions.isProviderFile
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

// TODO: Add a mapping or list of providers that have failed to initialize
//       and provide a way to retry initialization for those providers.
//       This will be useful to show a notification or a dialog to the user
//       to retry initialization of those providers.
internal class InitializeProvidersUseCaseImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val dataStoreManager: DataStoreManager,
        private val loadProviderUseCase: LoadProviderUseCase,
        private val appDispatchers: AppDispatchers,
    ) : InitializeProvidersUseCase {
        private suspend fun getProviderPrefs() =
            dataStoreManager
                .getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
                .first()

        override fun invoke() =
            channelFlow {
                withContext(appDispatchers.io) {
                    initializeDebugProviders()
                    initializeLocalProviders().collect { (provider, file) ->
                        loadProviderUseCase(
                            metadata = provider,
                            filePath = file.absolutePath,
                        ).collect(::send)
                    }
                }
            }

        /**
         * Initializes all debug providers from local storage and adds
         * them to the preferences, if they are not already present.
         * */
        private suspend fun initializeDebugProviders() {
            val path = "${context.getExternalFilesDir(null)}/$PROVIDERS_FOLDER_NAME/debug"
            val localDir = File(path)

            if (!localDir.exists()) {
                localDir.mkdirs()
                return
            }

            val repositoryDirectory = localDir.listFiles()

            repositoryDirectory?.forEach { subDirectory ->
                if (!subDirectory.isDirectory) return@forEach

                val updaterFile = File(subDirectory.absolutePath + "/${Constants.UPDATER_FILE}")
                if (!updaterFile.exists()) {
                    warnLog("Provider's `updater.json` could not be found!")
                    return@forEach
                }

                val updaterJson =
                    fromJson<List<ProviderMetadata>>(updaterFile.reader())

                val repository =
                    updaterJson
                        .firstOrNull()
                        ?.repositoryUrl
                        ?.toValidRepositoryLink()
                        ?: return@forEach

                val repositories = getProviderPrefs().repositories
                if (!repositories.contains(repository)) {
                    dataStoreManager.updateUserPrefs(
                        key = UserPreferences.PROVIDER_PREFS_KEY,
                        type = ProviderPreferences::class,
                    ) {
                        it.copy(repositories = it.repositories + repository)
                    }
                }

                val subFiles = subDirectory.listFiles()
                subFiles?.forEach subDirectory@{ providerFile ->
                    if (providerFile.name.equals(Constants.UPDATER_FILE, true)) {
                        return@subDirectory
                    }

                    val metadata =
                        updaterJson.find {
                            it.buildUrl.endsWith(providerFile.name)
                        } ?: return@subDirectory

                    addToPrefs(id = metadata.id, file = providerFile)
                }
            }
        }

        /**
         * Initializes all downloaded providers from local storage.
         * */
        private fun initializeLocalProviders() =
            channelFlow<Pair<ProviderMetadata, File>> {
                val providers = getProviderPrefs().providers

                if (providers.isEmpty()) {
                    infoLog("No providers found in preferences!")
                    return@channelFlow
                }

                providers.forEach { provider ->
                    val file = File(provider.filePath)

                    if (!file.exists()) {
                        warnLog("Provider file doesn't exist for: ${provider.name}")
                        return@forEach
                    }

                    val metadata = getMetadata(id = provider.id, file = file)
                        ?: return@forEach

                    val rootParentFolder = file.parentFile?.parentFile?.name
                    val isDebugProvider = rootParentFolder.equals(Constants.PROVIDER_DEBUG, true)

                    when {
                        file.isProviderFile && isDebugProvider && getProviderPrefs().shouldAddDebugPrefix -> {
                            val debugMetadata = metadata.copy(
                                id = "${metadata.id}-${Constants.PROVIDER_DEBUG}",
                                name = "${metadata.name}-${Constants.PROVIDER_DEBUG}",
                            )

                            send(debugMetadata to file)
                        }

                        file.isProviderFile -> {
                            send(metadata to file)
                        }

                        else -> {
                            // `file` could either be a dex file, an oat file, a json file, or a directory
                            // so for safety, we will delete the file using `rmrf`
                            rmrf(file)
                        }
                    }
                }
            }

        /**
         * Adds the debug provider to the preferences if it is not already present.
         *
         * @param id The unique identifier for the provider.
         * @param file The file representing the provider.
         * */
        private suspend fun addToPrefs(
            id: String,
            file: File,
        ) {
            val providers = getProviderPrefs().providers
            val isProviderNotYetLoaded = providers.none {
                it.filePath == file.absolutePath && it.id == id
            }

            if (isProviderNotYetLoaded) {
                val newProvider =
                    ProviderFromPreferences(
                        id = id,
                        name = file.nameWithoutExtension,
                        filePath = file.absolutePath,
                        isDisabled = false,
                        isDebug = true,
                    )

                dataStoreManager.updateUserPrefs(
                    key = UserPreferences.PROVIDER_PREFS_KEY,
                    type = ProviderPreferences::class,
                ) {
                    it.copy(providers = it.providers + newProvider)
                }
            }
        }

        /**
         * Retrieves the metadata from the `updater.json` file for the given provider ID.
         *
         * All online metadata is stored in the `updater.json` file
         *
         * @param id The unique identifier for the provider.
         * @param file The file representing the provider.
         * */
        private fun getMetadata(
            id: String,
            file: File,
        ): ProviderMetadata? {
            val updaterFilePath = file.parent?.plus("/${Constants.UPDATER_FILE}")

            if (updaterFilePath == null) {
                errorLog("Provider's file path must not be null!")
                return null
            }

            val updaterFile = File(updaterFilePath)

            if (!updaterFile.exists()) {
                errorLog("Provider's updater.json could not be found!")
                return null
            }

            val updaterJsonList = fromJson<List<ProviderMetadata>>(updaterFile.reader())

            return updaterJsonList.find { it.id == id }
        }
    }
