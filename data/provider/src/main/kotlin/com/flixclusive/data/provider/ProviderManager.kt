package com.flixclusive.data.provider

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.util.asStateFlow
import com.flixclusive.core.datastore.util.awaitFirst
import com.flixclusive.core.ui.common.util.showToast
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withDefaultContext
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
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
import com.flixclusive.data.provider.util.isCrashingOnGetApiMethod
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
import com.flixclusive.model.provider.Status
import com.flixclusive.provider.Provider
import dagger.hilt.android.qualifiers.ApplicationContext
import dalvik.system.PathClassLoader
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import java.io.File
import java.util.Collections
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
        private val providerApiRepository: ProviderApiRepository,
    ) {
        /** Map containing all loaded providers  */
        val providers: MutableMap<String, Provider> = Collections.synchronizedMap(LinkedHashMap())

        // TODO: Make this public for crash log purposes
        private val classLoaders: MutableMap<String, PathClassLoader> =
            Collections.synchronizedMap(HashMap())

        private var toast: Toast? = null

        /**
         * An observable map of provider data
         */
        val metadataList = mutableStateMapOf<String, ProviderMetadata>()

        /**
         *
         * Map of all repository's updater.json files and their contents
         * */
        private val updaterJsonMap = HashMap<String, List<ProviderMetadata>>()

        private val lock = Any()

        private val dynamicResourceLoader by lazy { DynamicResourceLoader(context = context) }

        private val localPathPrefixForDebug by lazy {
            "${context.getExternalDirPath()}/$PROVIDERS_FOLDER_NAME/debug"
        }

        val providerPreferencesAsState: StateFlow<ProviderPreferences>
            get() =
                dataStoreManager
                    .getUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY)
                    .asStateFlow(AppDispatchers.IO.scope)

        val providerPreferences: ProviderPreferences
            get() = providerPreferencesAsState.awaitFirst()

        val workingApis =
            snapshotFlow {
                metadataList
                    .mapNotNull { (id, data) ->
                        val api = providerApiRepository.get(id)

                        if (
                            data.status != Status.Maintenance &&
                            data.status != Status.Down &&
                            isProviderEnabled(id) &&
                            api != null
                        ) {
                            return@mapNotNull id to api
                        }

                        null
                    }
            }

        val workingProviders =
            snapshotFlow {
                metadataList
                    .mapNotNull { (id, data) ->
                        if (
                            data.status != Status.Maintenance &&
                            data.status != Status.Down &&
                            isProviderEnabled(id)
                        ) {
                            return@mapNotNull data
                        }

                        null
                    }
            }

        suspend fun initialize() {
            initializeDebugProviderFromPreferences()

            providerPreferencesAsState.value.providers.forEach { providerPreference ->
                val file = File(providerPreference.filePath)

                if (!file.exists()) {
                    warnLog("Provider file doesn't exist for: ${providerPreference.name}")
                    return@forEach
                }

                val metadata =
                    getProviderMetadataFromUpdater(
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
            val path = localPathPrefixForDebug
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

            val updaterJsonList =
                updaterJsonMap.getOrPut(updaterFile.absolutePath) {
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
            val updaterFile = File(saveTo.parent!!.plus(UPDATER_FILE))

            withIOContext {
                synchronized(lock) {
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
                        userId = userId,
                        providerId = provider.id,
                        providerRepository = provider.repositoryUrl.toValidRepositoryLink(),
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
            if (providers.contains(metadata.id)) {
                warnLog("Provider with name ${metadata.name} [${file.name}] already exists")
                return
            }

            infoLog("Loading provider: ${metadata.name} [${file.name}]")

            val filePath = file.absolutePath

            try {
                synchronized(lock) {
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

                    val providerFromPreferences =
                        getProviderFromPreferencesOrCreate(
                            id = metadata.id,
                            fileName = file.nameWithoutExtension,
                            filePath = filePath,
                        )

                    val needsMigration = providerFromPreferences.id.isEmpty()
                    if (needsMigration) {
                        migrateForOldSettingsFile(
                            directory = settingsDirPath,
                            metadata = metadata,
                        )
                    }

                    val provider =
                        loader.getProviderInstance(
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

                    if (!providerFromPreferences.isDisabled) {
                        val api = provider.getApi(context, client)

                        providerApiRepository.add(
                            id = metadata.id,
                            api = api,
                        )
                    }

                    providers[metadata.id] = provider
                    classLoaders[metadata.id] = loader
                    metadataList[metadata.id] = metadata
                }
            } catch (e: Throwable) {
                if (isCrashingOnGetApiMethod(e)) {
                    val message = context.getApiCrashMessage(provider = metadata.name)
                    showToastOnProviderCrash(message)
                    errorLog(message)

                    toggleUsageOnSettings(
                        id = metadata.id,
                        isDisabled = true,
                    )

                    return loadProvider(file, metadata)
                }

                val message = context.getCommonCrashMessage(provider = metadata.name)
                showToastOnProviderCrash(message)
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

            val newSettingsFile = File(oldSettingsFile, metadata.id)
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

        private fun showToastOnProviderCrash(message: String) {
            if (toast != null) {
                toast!!.cancel()
            }

            toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
            toast!!.show()
        }

        private suspend fun loadOnPreferences(provider: ProviderFromPreferences) {
            val index =
                providerPreferencesAsState.value
                    .providers
                    .indexOfFirst { it.id == provider.id }

            updateProviderPrefs {
                val providersList = it.providers.toMutableList()

                if (index > -1) {
                    providersList[index] = provider
                } else {
                    providersList.add(provider)
                }

                it.copy(
                    providers = providersList.toList(),
                )
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

            val provider = providers[metadata.id]
            val file = File(providerFromPreferences.filePath)

            if (provider == null || !file.exists()) {
                errorLog("Provider [${metadata.name}] not found. Cannot be unloaded")
                return
            }

            infoLog("Unloading provider: ${provider.name}")
            safeCall("Exception while unloading provider with ID: ${provider.name}") {
                provider.onUnload(context)
            }

            removeProviderFromListsAndMaps(id = metadata.id)
            deleteProviderRelatedFiles(file = file)

            if (unloadOnPreferences) {
                unloadOnPreferences(id = metadata.id)
            }
        }

        private fun removeProviderFromListsAndMaps(id: String) {
            synchronized(lock) {
                metadataList.remove(id)
                classLoaders.remove(id)
                providerApiRepository.remove(id)
                providers.remove(id)
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

        private suspend fun unloadOnPreferences(id: String) {
            updateProviderPrefs {
                val newList = it.providers.toMutableList()
                newList.removeIf { providerPref ->
                    providerPref.id == id
                }

                it.copy(providers = newList)
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
            return oldPreference.copy(
                name = newMetadata.name,
                filePath =
                    context
                        .createFileForProvider(
                            userId = userId,
                            providerId = newMetadata.id,
                            providerRepository = newMetadata.repositoryUrl.toValidRepositoryLink(),
                        ).absolutePath,
            )
        }

        @Throws(DownloadFailed::class)
        suspend fun update(
            oldMetadata: ProviderMetadata,
            newMetadata: ProviderMetadata,
        ) {
            require(providers.contains(oldMetadata.id)) {
                "No such provider: ${oldMetadata.name}"
            }

            val newPreference =
                createNewProviderFromPreferences(
                    id = oldMetadata.id,
                    newMetadata = newMetadata,
                )

            loadOnPreferences(provider = newPreference)

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

        suspend fun swapOrder(
            from: Int,
            to: Int,
        ) {
            updateProviderPrefs {
                val newProvidersOrder = it.providers.toMutableList()
                val size = newProvidersOrder.size

                if (from == to || from < 0 || to < 0 || from >= size || to >= size) {
                    return@updateProviderPrefs it
                }

                val tempProviderPreference = newProvidersOrder[from]
                newProvidersOrder[from] = newProvidersOrder[to]
                newProvidersOrder[to] = tempProviderPreference

                it.copy(providers = newProvidersOrder)
            }
        }

        /**
         * Toggles a provider. If it is enabled, it will be disabled and vice versa.
         *
         * @param metadata The data of the provider to toggle
         */
        suspend fun toggleUsage(metadata: ProviderMetadata) {
            val isProviderEnabled = isProviderEnabled(metadata.id)
            toggleUsageOnSettings(
                id = metadata.id,
                isDisabled = isProviderEnabled,
            )

            if (isProviderEnabled) {
                providerApiRepository.remove(metadata.id)
            } else {
                try {
                    val api =
                        providers[metadata.id]
                            ?.getApi(
                                context = context,
                                client = client,
                            )

                    if (api != null) {
                        providerApiRepository.add(
                            id = metadata.id,
                            api = api,
                        )
                    }
                } catch (e: Throwable) {
                    val message = context.getApiCrashMessage(provider = metadata.name)
                    errorLog(e)
                    errorLog(message)
                    context.showToast(message)

                    toggleUsage(metadata)
                }
            }
        }

        private suspend fun toggleUsageOnSettings(
            id: String,
            isDisabled: Boolean,
        ) {
            updateProviderPrefs {
                withDefaultContext {
                    val listOfSavedProviders = it.providers.toMutableList()

                    val indexOfProvider =
                        listOfSavedProviders.indexOfFirst { provider ->
                            provider.id == id
                        }
                    val provider = listOfSavedProviders[indexOfProvider]

                    listOfSavedProviders[indexOfProvider] = provider.copy(isDisabled = isDisabled)

                    it.copy(providers = listOfSavedProviders.toList())
                }
            }
        }

        /**
         * Checks whether a provider is enabled
         *
         * @param id Name of the provider
         * @return Whether the provider is enabled
         */
        fun isProviderEnabled(id: String): Boolean =
            // TODO: REMOVE `isProviderEnabled` HERE. IT'S NOT RELATED TO THE OTHER FUNCTIONS
            providerPreferences.providers
                .find { it.id == id }
                ?.isDisabled
                ?.not() != false

        private suspend fun updateProviderPrefs(transform: suspend (t: ProviderPreferences) -> ProviderPreferences) {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(
                key = UserPreferences.PROVIDER_PREFS_KEY,
                transform = transform,
            )
        }
    }
