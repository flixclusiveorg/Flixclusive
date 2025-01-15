package com.flixclusive.domain.provider

import android.content.Context
import android.widget.Toast
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.PROVIDERS_FOLDER_NAME
import com.flixclusive.core.datastore.PROVIDERS_SETTINGS_FOLDER_NAME
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.util.awaitFirst
import com.flixclusive.core.datastore.util.getExternalDirPath
import com.flixclusive.core.datastore.util.rmrf
import com.flixclusive.core.ui.common.util.showToast
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withMainContext
import com.flixclusive.core.util.coroutines.blockFirstNotNull
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.data.provider.ProviderApiRepository
import com.flixclusive.data.provider.ProviderRepository
import com.flixclusive.domain.provider.util.DynamicResourceLoader
import com.flixclusive.domain.provider.util.createFileForProvider
import com.flixclusive.domain.provider.util.downloadProvider
import com.flixclusive.domain.provider.util.getApiCrashMessage
import com.flixclusive.domain.provider.util.getCommonCrashMessage
import com.flixclusive.domain.provider.util.getFileFromPath
import com.flixclusive.domain.provider.util.getProviderInstance
import com.flixclusive.domain.provider.util.isClassesDex
import com.flixclusive.domain.provider.util.isJson
import com.flixclusive.domain.provider.util.isNotOat
import com.flixclusive.domain.provider.util.isProviderFile
import com.flixclusive.model.datastore.user.ProviderFromPreferences
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.provider.ProviderManifest
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import dagger.hilt.android.qualifiers.ApplicationContext
import dalvik.system.PathClassLoader
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import com.flixclusive.core.locale.R as LocaleR

internal const val UPDATER_FILE = "updater.json"
internal const val PROVIDER_DEBUG = "debug"

private const val MANIFEST_FILE = "manifest.json"

@Singleton
class ProviderLoaderUseCase
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val client: OkHttpClient,
        private val userSessionDataStore: UserSessionDataStore,
        private val dataStoreManager: DataStoreManager,
        private val providerRepository: ProviderRepository,
        private val providerApiRepository: ProviderApiRepository,
    ) {
        private var toast: Toast? = null

        private val dynamicResourceLoader by lazy { DynamicResourceLoader(context = context) }

        private val providerPreferences: ProviderPreferences get() =
            dataStoreManager
                .getUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY)
                .awaitFirst()

        suspend fun load(
            provider: ProviderMetadata,
            needsDownload: Boolean = false,
            filePath: String? = null,
        ) {
            val userId = userSessionDataStore.currentUserId.blockFirstNotNull()!!
            val file =
                filePath?.let { File(it) }
                    ?: context.createFileForProvider(
                        provider = provider,
                        userId = userId,
                    )

            if (needsDownload) {
                client.downloadProvider(file, provider.buildUrl)
            }

            load(
                file = file,
                metadata = provider,
            )
        }

        suspend fun initDebugFolderToPreferences() {
            val path = "${context.getExternalDirPath()}/$PROVIDERS_FOLDER_NAME/debug"
            val localDir = File(path)

            if (!localDir.exists()) {
                localDir.mkdirs()
                return
            }

            val repositoryDirectory = localDir.listFiles()

            repositoryDirectory?.forEach { subDirectory ->
                if (!subDirectory.isDirectory) return@forEach

                val updaterFile = File(subDirectory.absolutePath + "/$UPDATER_FILE")
                if (!updaterFile.exists()) {
                    warnLog("Provider's `updater.json` could not be found!")
                    return@forEach
                }

                val repository =
                    fromJson<List<ProviderMetadata>>(updaterFile.reader())
                        .firstOrNull()
                        ?.repositoryUrl
                        ?.toValidRepositoryLink()
                        ?: return@forEach

                if (!providerPreferences.repositories.contains(repository)) {
                    updateProviderPrefs {
                        it.copy(repositories = it.repositories + repository)
                    }
                }

                subDirectory.listFiles()?.forEach subDirectory@{ providerFile ->
                    if (!providerFile.name.equals(UPDATER_FILE, true)) {
                        return@subDirectory
                    }

                    addProviderToPreferences(file = providerFile)
                }
            }
        }

        suspend fun initFromLocal() {
            providerPreferences.providers.forEach { itemPreference ->
                val file = File(itemPreference.filePath)

                if (!file.exists()) {
                    warnLog("Provider file doesn't exist for: ${itemPreference.name}")
                    return
                }

                val metadata =
                    getProviderMetadataFromUpdater(
                        id = itemPreference.id,
                        file = file,
                    ) ?: return

                val isDebugProvider = file.parent?.equals(PROVIDER_DEBUG, true) == true

                when {
                    file.isProviderFile && isDebugProvider -> {
                        loadDebugProvider(
                            file = file,
                            metadata = metadata,
                        )
                    }

                    file.isProviderFile -> {
                        load(
                            file = file,
                            metadata = metadata,
                        )
                    }

                    file.isNotOat && file.isDirectory -> {
                        // Some roms create this
                        context.showToast(
                            String.format(
                                context.getString(LocaleR.string.invalid_provider_file_directory_msg_format),
                                file.name,
                            ),
                        )

                        rmrf(file)
                    }

                    file.isNotOat || file.isClassesDex || file.isJson -> {
                        // Some roms create this
                        context.showToast(
                            String.format(
                                context.getString(LocaleR.string.invalid_provider_file_dex_json_msg_format),
                                file.name,
                            ),
                        )

                        rmrf(file)
                    }
                }
            }
        }

        private suspend fun addProviderToPreferences(file: File) {
            val isProviderNotYetLoaded =
                providerPreferences.providers
                    .any { it.filePath == file.absolutePath }
                    .not()

            if (isProviderNotYetLoaded) {
                val newProvider =
                    ProviderFromPreferences(
                        name = file.nameWithoutExtension,
                        filePath = file.absolutePath,
                        isDisabled = false,
                        isDebug = true,
                    )

                updateProviderPrefs {
                    it.copy(providers = it.providers + newProvider)
                }
            }
        }

        private fun getProviderMetadataFromUpdater(
            id: String,
            file: File,
        ): ProviderMetadata? {
            val updaterFilePath = file.parent?.plus("/$UPDATER_FILE")

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

        /**
         * Loads a provider
         *
         * @param file              Provider file
         * @param metadata      The provider information
         */
        @Suppress("UNCHECKED_CAST")
        private suspend fun load(
            file: File,
            metadata: ProviderMetadata,
        ) {
            if (providerRepository.getProvider(metadata.id) != null) {
                warnLog("Provider with name ${metadata.name} [${file.name}] already exists")
                return
            }

            infoLog("Loading provider: ${metadata.name} [${file.name}]")

            val filePath = file.absolutePath

            try {
                safeCall {
                    File(filePath).setReadOnly()
                }

                val loader = PathClassLoader(filePath, context.classLoader)
                val manifest: ProviderManifest = loader.getFileFromPath(MANIFEST_FILE)
                val settingsDirPath =
                    createSettingsDirPath(
                        repositoryUrl = metadata.repositoryUrl,
                        isDebugProvider = metadata.id.endsWith(PROVIDER_DEBUG),
                    )

                val preferenceItem =
                    getPreferenceItemOrCreate(
                        id = metadata.id,
                        fileName = file.nameWithoutExtension,
                        filePath = filePath,
                    )

                if (canMigrateSettingsFile(metadata)) {
                    migrateForOldSettingsFile(
                        directory = settingsDirPath,
                        metadata = metadata,
                    )
                }

                val provider =
                    loader.getProviderInstance(
                        id = metadata.id,
                        file = file,
                        manifest = manifest,
                        settingsDirPath = settingsDirPath,
                    )

                if (manifest.requiresResources) {
                    provider.resources = dynamicResourceLoader.load(inputFile = file)
                }

                if (manifest.requiresResources && dynamicResourceLoader.isAPI23OrBelow()) {
                    dynamicResourceLoader.cleanupArtifacts(file)
                }

                var isApiDisabled = preferenceItem.isDisabled
                try {
                    if (!isApiDisabled) {
                        providerApiRepository.addApiFromProvider(
                            id = metadata.id,
                            provider = provider,
                        )
                    }
                } catch (_: Exception) {
                    isApiDisabled = true

                    val message = context.getApiCrashMessage(provider = metadata.name)
                    context.showToastOnProviderCrash(message)
                    errorLog(message)
                } finally {
                    providerRepository.add(
                        classLoader = loader,
                        provider = provider,
                        metadata = metadata,
                        preferenceItem = preferenceItem.copy(isDisabled = isApiDisabled),
                    )
                }
            } catch (e: Throwable) {
                val message = context.getCommonCrashMessage(provider = metadata.name)
                context.showToastOnProviderCrash(message)
                errorLog("${metadata.name} crashed with error!")
                errorLog(e)
            }
        }

        @Suppress("UNCHECKED_CAST")
        private suspend fun loadDebugProvider(
            file: File,
            metadata: ProviderMetadata,
        ) {
            load(
                file = file,
                metadata =
                    metadata.copy(
                        id = "${metadata.id}-$PROVIDER_DEBUG",
                        name = "${metadata.name}-$PROVIDER_DEBUG",
                    ),
            )
        }

        private fun canMigrateSettingsFile(metadata: ProviderMetadata): Boolean {
            val providerFromPreference =
                providerPreferences.providers
                    .find { it.name.equals(metadata.name, true) }
                    ?: return false

            return providerFromPreference.id.isEmpty()
        }

        private fun migrateForOldSettingsFile(
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

        private fun createSettingsDirPath(
            repositoryUrl: String,
            isDebugProvider: Boolean,
        ): String {
            val userId = userSessionDataStore.currentUserId.blockFirstNotNull()!!
            val parentDirectoryName = if (isDebugProvider) PROVIDER_DEBUG else "user-$userId"

            val repository = repositoryUrl.toValidRepositoryLink()
            val childDirectoryName = "${repository.owner}-${repository.name}"
            val finalPathPrefix = "$PROVIDERS_SETTINGS_FOLDER_NAME/$parentDirectoryName/$childDirectoryName"

            return "${context.getExternalDirPath()}/$finalPathPrefix"
        }

        private fun getPreferenceItemOrCreate(
            id: String,
            fileName: String,
            filePath: String,
        ): ProviderFromPreferences {
            var providerFromPreferences =
                providerPreferences.providers
                    .find { it.id == id }

            if (providerFromPreferences == null) {
                providerFromPreferences =
                    ProviderFromPreferences(
                        id = id,
                        name = fileName,
                        filePath = filePath,
                        isDisabled = false,
                    )
            }

            return providerFromPreferences
        }

        private suspend fun Context.showToastOnProviderCrash(message: String) {
            withMainContext {
                if (toast != null) {
                    toast!!.cancel()
                }

                toast = Toast.makeText(this@showToastOnProviderCrash, message, Toast.LENGTH_SHORT)
                toast!!.show()
            }
        }

        private suspend fun updateProviderPrefs(transform: suspend (t: ProviderPreferences) -> ProviderPreferences) {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(
                key = UserPreferences.PROVIDER_PREFS_KEY,
                transform = transform,
            )
        }
    }
