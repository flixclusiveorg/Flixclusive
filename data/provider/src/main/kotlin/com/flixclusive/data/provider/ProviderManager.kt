package com.flixclusive.data.provider

import android.content.Context
import android.widget.Toast
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.util.asStateFlow
import com.flixclusive.core.datastore.util.awaitFirst
import com.flixclusive.core.ui.common.util.showToast
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withDefaultContext
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withMainContext
import com.flixclusive.core.util.coroutines.blockFirstNotNull
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.data.provider.util.DownloadFailed
import com.flixclusive.data.provider.util.DynamicResourceLoader
import com.flixclusive.data.provider.util.createFileForProvider
import com.flixclusive.data.provider.util.download
import com.flixclusive.data.provider.util.getApiCrashMessage
import com.flixclusive.data.provider.util.getCommonCrashMessage
import com.flixclusive.data.provider.util.getExternalDirPath
import com.flixclusive.data.provider.util.getFileFromPath
import com.flixclusive.data.provider.util.getProviderInstance
import com.flixclusive.data.provider.util.isClassesDex
import com.flixclusive.data.provider.util.isJson
import com.flixclusive.data.provider.util.isNotOat
import com.flixclusive.data.provider.util.isProviderFile
import com.flixclusive.data.provider.util.replaceLastAfterSlash
import com.flixclusive.data.provider.util.rmrf
import com.flixclusive.model.datastore.user.ProviderFromPreferences
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.provider.ProviderManifest
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import dagger.hilt.android.qualifiers.ApplicationContext
import dalvik.system.PathClassLoader
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import com.flixclusive.core.locale.R as LocaleR

internal const val PROVIDERS_FOLDER_NAME = "providers"
internal const val PROVIDERS_SETTINGS_FOLDER_NAME = "settings"
const val PROVIDER_DEBUG = "debug"

private const val MANIFEST_FILE = "manifest.json"
private const val UPDATER_FILE = "updater.json"

@Singleton
class ProviderManager
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

        val providerPreferencesAsState: StateFlow<ProviderPreferences>
            get() =
                dataStoreManager
                    .getUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY)
                    .asStateFlow(AppDispatchers.IO.scope)

        val providerPreferences: ProviderPreferences get() = providerPreferencesAsState.awaitFirst()

        suspend fun initialize() {
            providerRepository.initializeOrder()
            initializeDebugProviderFromPreferences()

            val updaterJsonMap = HashMap<String, List<ProviderMetadata>>()
            providerPreferencesAsState.value.providers.forEach { providerPreference ->
                val file = File(providerPreference.filePath)

                if (!file.exists()) {
                    warnLog("Provider file doesn't exist for: ${providerPreference.name}")
                    return@forEach
                }

                val metadata =
                    updaterJsonMap.getProviderMetadataFromUpdater(
                        id = providerPreference.id,
                        file = file,
                    ) ?: return@forEach

                initializeFromFile(
                    file = file,
                    metadata = metadata,
                )
            }
        }

        private suspend fun initializeDebugProviderFromPreferences() {
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

                subDirectory.listFiles()?.forEach { providerFile ->
                    if (!providerFile.name.equals(UPDATER_FILE, true)) {
                        return@forEach
                    }

                    addProviderToPreferences(file = providerFile)
                }
            }
        }

        private suspend fun addProviderToPreferences(file: File) {
            val isProviderNotYetLoaded =
                providerPreferencesAsState.value.providers
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

        private fun HashMap<String, List<ProviderMetadata>>.getProviderMetadataFromUpdater(
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

            val updaterJsonList =
                getOrPut(updaterFile.absolutePath) {
                    fromJson<List<ProviderMetadata>>(updaterFile.reader())
                }

            return updaterJsonList.find { it.id == id }
        }

        private suspend fun initializeFromFile(
            file: File,
            metadata: ProviderMetadata,
        ) {
            val isDebugProvider = file.parent?.equals(PROVIDER_DEBUG, true) == true
            when {
                file.isProviderFile && isDebugProvider -> {
                    loadDebugProvider(
                        file = file,
                        metadata = metadata,
                    )
                }

                file.isProviderFile -> {
                    loadProvider(
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

        @Throws(DownloadFailed::class)
        private suspend fun downloadProvider(
            saveTo: File,
            buildUrl: String,
        ) {
            val updaterJsonUrl = replaceLastAfterSlash(buildUrl, UPDATER_FILE)
            val updaterFile = File(saveTo.parent!!.plus("/$UPDATER_FILE"))

            withIOContext {
                client.download(
                    file = saveTo,
                    downloadUrl = buildUrl,
                )

                client.download(
                    file = updaterFile,
                    downloadUrl = updaterJsonUrl,
                )
            }
        }

        suspend fun loadProvider(
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
                downloadProvider(file, provider.buildUrl)
            }

            loadProvider(
                file = file,
                metadata = provider,
            )
        }

        /**
         * Loads a provider
         *
         * @param file              Provider file
         * @param metadata      The provider information
         */
        @Suppress("UNCHECKED_CAST")
        private suspend fun loadProvider(
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
                    getProviderFromPreferencesOrCreate(
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

                try {
                    if (!preferenceItem.isDisabled) {
                        val api = provider.getApi(context, client)

                        providerApiRepository.addApi(
                            id = metadata.id,
                            api = api,
                        )
                    }
                } finally {
                    // TODO: Fix isDisabled property here
                    val message = context.getApiCrashMessage(provider = metadata.name)
                    context.showToastOnProviderCrash(message)
                    errorLog(message)

                    providerRepository.add(
                        classLoader = loader,
                        provider = provider,
                        metadata = metadata,
                        preferenceItem = preferenceItem.copy(isDisabled = true),
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
            loadProvider(
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
                providerPreferencesAsState.value.providers
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

        private fun getProviderFromPreferencesOrCreate(
            id: String,
            fileName: String,
            filePath: String,
        ): ProviderFromPreferences {
            var providerFromPreferences =
                providerPreferencesAsState.value.providers
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
                    providerPreferencesAsState.value
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

        private fun createNewProviderFromPreferences(
            id: String,
            newMetadata: ProviderMetadata,
        ): ProviderFromPreferences {
            val oldOrderPosition =
                providerPreferencesAsState.value
                    .providers
                    .indexOfFirst { it.id == id }

            val oldPreference = providerPreferences.providers[oldOrderPosition]

            val userId = userSessionDataStore.currentUserId.blockFirstNotNull()!!
            val file =
                context
                    .createFileForProvider(
                        userId = userId,
                        provider = newMetadata,
                    )
            val filePath = file.absolutePath

            return oldPreference.copy(
                name = newMetadata.name,
                filePath = filePath,
            )
        }

        @Throws(DownloadFailed::class)
        suspend fun update(
            oldMetadata: ProviderMetadata,
            newMetadata: ProviderMetadata,
        ) {
            requireNotNull(providerRepository.getProvider(oldMetadata.id)) {
                "No such provider: ${oldMetadata.name}"
            }

            val newPreference =
                createNewProviderFromPreferences(
                    id = oldMetadata.id,
                    newMetadata = newMetadata,
                )

            providerRepository.addToPreferences(preferenceItem = newPreference)

            downloadProvider(
                saveTo = File(newPreference.filePath),
                buildUrl = newMetadata.buildUrl,
            )

            unload(
                metadata = oldMetadata,
                unloadOnPreferences = false,
            )

            loadProvider(
                provider = newMetadata,
                filePath = newPreference.filePath,
            )
        }

        private suspend fun updateProviderPrefs(transform: suspend (t: ProviderPreferences) -> ProviderPreferences) {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(
                key = UserPreferences.PROVIDER_PREFS_KEY,
                transform = transform,
            )
        }
    }
