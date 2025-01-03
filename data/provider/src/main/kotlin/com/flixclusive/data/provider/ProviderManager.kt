package com.flixclusive.data.provider

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.util.asStateFlow
import com.flixclusive.core.datastore.util.awaitFirst
import com.flixclusive.core.ui.common.util.showToast
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withDefaultContext
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.data.provider.util.CrashHelper.getApiCrashMessage
import com.flixclusive.data.provider.util.CrashHelper.isCrashingOnGetApiMethod
import com.flixclusive.data.provider.util.DynamicResourceLoader
import com.flixclusive.data.provider.util.NotificationUtil.notifyOnError
import com.flixclusive.data.provider.util.buildValidFilename
import com.flixclusive.data.provider.util.downloadFile
import com.flixclusive.data.provider.util.provideValidProviderPath
import com.flixclusive.data.provider.util.replaceLastAfterSlash
import com.flixclusive.data.provider.util.rmrf
import com.flixclusive.model.datastore.user.ProviderOrderEntity
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.provider.ProviderData
import com.flixclusive.model.provider.ProviderManifest
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import com.flixclusive.model.provider.Status
import com.flixclusive.provider.Provider
import com.flixclusive.provider.settings.ProviderSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import dalvik.system.PathClassLoader
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton
import com.flixclusive.core.locale.R as LocaleR


const val PROVIDERS_FOLDER = "flx_providers"
private const val UPDATER_JSON_FILE = "/updater.json"

@Singleton
class ProviderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStoreManager: DataStoreManager,
    private val client: OkHttpClient,
    private val providerApiRepository: ProviderApiRepository
) {
    /** Map containing all loaded providers  */
    val providers: MutableMap<String, Provider> = Collections.synchronizedMap(LinkedHashMap())
    private val classLoaders: MutableMap<PathClassLoader, Provider> = Collections.synchronizedMap(HashMap())

    private var notificationChannelHasBeenInitialized = false

    /**
     * An observable map of provider data
     */
    val providerDataList = mutableStateListOf<ProviderData>()

    /** Providers that failed to load for various reasons. Map of provider data to String or Exception  */
    private val failedToLoad: MutableMap<ProviderData, Any> = LinkedHashMap()

    /**
     *
     * Map of all repository's updater.json files and their contents
     * */
    private val updaterJsonMap = HashMap<String, List<ProviderData>>()

    private val dynamicResourceLoader = DynamicResourceLoader(context = context)
    private val LOCAL_PATH_PREFIX = context.getExternalFilesDir(null)?.absolutePath + "/providers/"

    val providerPreferencesAsState get() = dataStoreManager
        .getUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY)
        .asStateFlow(AppDispatchers.IO.scope)
    
    val providerPreferences get() = providerPreferencesAsState.awaitFirst()
    
    val workingApis = snapshotFlow {
        providerDataList
            .mapNotNull { data ->
                val api = providerApiRepository.apiMap[data.name]

                if (
                    data.status != Status.Maintenance
                    && data.status != Status.Down
                    && isProviderEnabled(data.name)
                    && api != null
                ) return@mapNotNull api

                null
            }
    }
    
    val workingProviders = snapshotFlow {
        providerDataList
            .mapNotNull { data ->
                if (
                    data.status != Status.Maintenance
                    && data.status != Status.Down
                    && isProviderEnabled(data.name)
                ) return@mapNotNull data

                null
            }
    }

    suspend fun initialize() {
        initializeLocalProviders()

        providerPreferences.providers.forEach { providerPreference ->
            val file = File(providerPreference.filePath)

            if (!file.exists()) {
                warnLog("Provider file doesn't exist for: ${providerPreference.name}")
                return@forEach
            }

            initializeProvider(providerFile = file)
        }

        if (failedToLoad.isNotEmpty()) {
            context.notifyOnError(
                shouldInitializeChannel = !notificationChannelHasBeenInitialized,
                providers = failedToLoad.keys,
            )

            notificationChannelHasBeenInitialized = true
        }
    }

    private suspend fun initializeLocalProviders() {
        val localDir = File(LOCAL_PATH_PREFIX)

        if (!localDir.exists()) {
            val isSuccess = localDir.mkdirs()
            if (!isSuccess) {
                warnLog("Failed to create local directories when loading providers.")
            }

            return
        }
        
        val repositoryFolders = localDir.listFiles()

        repositoryFolders?.forEach folderForEach@ { folder ->
            if (!folder.isDirectory)
                return@folderForEach

            val updaterJsonFile = File(folder.absolutePath + UPDATER_JSON_FILE)
            if (!updaterJsonFile.exists()) {
                errorLog("Provider's updater.json could not be found!")
                return@folderForEach
            }

            val randomProviderFile = fromJson<List<ProviderData>>(updaterJsonFile.reader())
                .firstOrNull()
                ?: return@folderForEach

            val repository = randomProviderFile.repositoryUrl?.toValidRepositoryLink()
                ?: return@folderForEach

            if (!providerPreferences.repositories.contains(repository)) {
                dataStoreManager.updateUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY) {
                    it.copy(repositories = it.repositories + repository)
                }
            }

            folder.listFiles()
                ?.forEach { providerFile ->
                    val isProviderNotYetLoaded
                        = providerPreferences.providers.any {
                            it.filePath == providerFile.absolutePath
                        }.not()
                    val isNotUpdaterJsonFile
                        = !providerFile.name
                            .equals("updater.json", true)

                    if (isProviderNotYetLoaded && isNotUpdaterJsonFile) {
                        dataStoreManager.updateUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY) {
                            it.copy(
                                providers =
                                    it.providers + ProviderOrderEntity(
                                        name = providerFile.nameWithoutExtension,
                                        filePath = providerFile.absolutePath,
                                        isDisabled = false
                                    )
                            )
                        }
                    }
                }
        }
    }

    private suspend fun initializeProvider(providerFile: File) {
        val updaterJsonFilePath = providerFile.parent?.plus(UPDATER_JSON_FILE)

        if (updaterJsonFilePath == null) {
            errorLog("Provider's file path must not be null!")
            return
        }

        val updaterJsonFile = File(updaterJsonFilePath)

        if (!updaterJsonFile.exists()) {
            errorLog("Provider's updater.json could not be found!")
            return
        }

        val updaterJsonList = updaterJsonMap.getOrPut(updaterJsonFile.absolutePath) {
            fromJson<List<ProviderData>>(updaterJsonFile.reader())
        }

        val providerName = providerFile.name
        val providerData = updaterJsonList.find { it.name.plus(".flx").equals(providerName, true) }

        if (providerData == null) {
            errorLog("Provider [$providerName] cannot be found on the updater.json file!")
            return
        }

        if (providerName.endsWith(".flx")) {
            loadProvider(
                file = providerFile,
                providerData = providerData
            )
        } else if (providerName != "oat") { // Some roms create this
            if (providerFile.isDirectory) {
                context.showToast(
                    String.format(context.getString(LocaleR.string.invalid_provider_file_directory_msg_format), providerName)
                )
            } else if (providerName.equals("classes.dex") || providerName.endsWith(".json")) {
                context.showToast(
                    String.format(context.getString(LocaleR.string.invalid_provider_file_dex_json_msg_format), providerName)
                )
            }
            rmrf(providerFile)
        }
    }

    private suspend fun downloadProvider(
        file: File,
        buildUrl: String
    ): Boolean {
        val updaterJsonUrl = replaceLastAfterSlash(buildUrl, "updater.json")
        val updaterJsonFile = File(file.parent!!.plus(UPDATER_JSON_FILE))

        // Download provider
        val isProviderDownloadSuccess = withIOContext {
            client.downloadFile(
                file = file, downloadUrl = buildUrl
            )
        }

        // Download updater.json
        val isUpdaterJsonDownloadSuccess = withIOContext {
            client.downloadFile(
                file = updaterJsonFile, downloadUrl = updaterJsonUrl
            )
        }

        return isProviderDownloadSuccess && isUpdaterJsonDownloadSuccess
    }

    suspend fun loadProvider(
        providerData: ProviderData,
        needsDownload: Boolean = false,
        filePath: String? = null
    ) {
        check(providerData.repositoryUrl != null) {
            "Repository URL must not be null if using this overloaded method."
        }

        val file = filePath?.let { File(it) } 
            ?: context.provideValidProviderPath(providerData)

        if (needsDownload && !downloadProvider(file, providerData.buildUrl!!)) {
            throw IOException("Something went wrong trying to download the provider.")
        }

        val initialFailedToLoadProviders = failedToLoad.size
        loadProvider(
            file = file,
            providerData = providerData
        )

        val hasNewErrors = needsDownload && failedToLoad.size - initialFailedToLoadProviders > 0
        if (hasNewErrors) {
            context.notifyOnError(
                shouldInitializeChannel = !notificationChannelHasBeenInitialized,
                providers = failedToLoad.keys,
            )

            notificationChannelHasBeenInitialized = true
        }
    }

    /**
     * Loads a provider
     *
     * @param file              Provider file
     * @param providerData      The provider information
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun loadProvider(
        file: File,
        providerData: ProviderData
    ) {
        val name = file.nameWithoutExtension
        val filePath = file.absolutePath

        val providerPosition = getPositionIndexFromSettings(
            name = name
        )

        infoLog("Loading provider: $name")

        safeCall {
            File(filePath).setReadOnly()
        }

        try {
            val loader = PathClassLoader(filePath, context.classLoader)
            var manifest: ProviderManifest

            loader.getResourceAsStream("manifest.json").use { stream ->
                if (stream == null) {
                    throw NullPointerException("No manifest found")
                }
                InputStreamReader(stream).use { reader ->
                    manifest = fromJson(reader)
                }
            }

            val providerClass: Class<out Provider?> =
                loader.loadClass(manifest.providerClassName) as Class<out Provider>
            val providerInstance: Provider =
                providerClass.getDeclaredConstructor().newInstance() as Provider

            if (providers.containsKey(name)) {
                errorLog("Provider with name $name already exists")
                return
            }

            val settingsPath = context.getExternalFilesDir(null)
                ?.absolutePath + "/settings/${buildValidFilename(providerData.repositoryUrl!!)}"

            providerInstance.__filename = name
            providerInstance.manifest = manifest
            providerInstance.settings = ProviderSettings(
                fileDirectory = settingsPath,
                providerName = providerData.name
            )
            if (manifest.requiresResources) {
                withIOContext {
                    dynamicResourceLoader.load(
                        inputFile = file,
                        provider = providerInstance
                    )
                }
            }

            val providerOrderEntity = if (providerPosition > -1) {
                providerPreferences.providers[providerPosition]
            } else {
                ProviderOrderEntity(
                    name = name,
                    filePath = filePath,
                    isDisabled = false
                ).also {
                    loadProviderOnSettings(it)
                }
            }

            if (!providerOrderEntity.isDisabled) {
                val api = providerInstance.getApi(context, client)

                providerApiRepository.add(
                    providerName = providerData.name,
                    providerApi = api
                )
            }

            providers[name] = providerInstance
            classLoaders[loader] = providerInstance

            if (providerPosition > -1) {
                providerDataList.add(
                    index = providerPosition, element = providerData
                )
            } else {
                providerDataList.add(element = providerData)
            }
        } catch (e: Throwable) {
            if (isCrashingOnGetApiMethod(e)) {
                val message = context.getApiCrashMessage(provider = name)
                errorLog(message)

                toggleUsageOnSettings(
                    name = name,
                    isDisabled = true
                )

                return loadProvider(file, providerData)
            }

            errorLog("$name crashed with error: ${e.localizedMessage}")
            errorLog(e)
            failedToLoad[providerData] = e
        }
    }

    private suspend fun loadProviderOnSettings(
        provider: ProviderOrderEntity,
        index: Int = -1
    ) {
        dataStoreManager.updateUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY) {
            val providersList = it.providers.toMutableList()

            if (index > -1) {
                providersList[index] = provider
            } else {
                providersList.add(provider)
            }

            it.copy(
                providers = providersList.toList()
            )
        }
    }

    /**
     * Unloads a provider
     *
     * @param providerData the [ProviderData] to uninstall/unload
     * @param unloadOnSettings an optional toggle to also unload the provider from the settings. Default value is *true*
     */
    suspend fun unloadProvider(
        providerData: ProviderData,
        unloadOnSettings: Boolean = true
    ) {
        val index = getPositionIndexFromSettings(providerData.name)
        val providerPreference = providerPreferences.providers[index]

        unloadProvider(
            providerOrderEntity = providerPreference,
            unloadOnSettings = unloadOnSettings
        )
    }

    /**
     *
     * Unloads a provider based on [ProviderOrderEntity]
     *
     * @param providerOrderEntity the [ProviderOrderEntity] required that contains the file path and provider's name
     * @param unloadOnSettings an optional toggle to also unload the provider from the settings. Default value is *true*
     * */
    suspend fun unloadProvider(
        providerOrderEntity: ProviderOrderEntity,
        unloadOnSettings: Boolean = true
    ) {
        val provider = providers[providerOrderEntity.name]
        val file = File(providerOrderEntity.filePath)

        if (provider == null || !file.exists()) {
            errorLog("Provider [${providerOrderEntity.name}] not found. Cannot be unloaded")
            return
        }

        unloadProvider(provider, file, unloadOnSettings)
    }

    private suspend fun unloadProvider(
        provider: Provider,
        file: File,
        unloadOnSettings: Boolean
    ) {
        infoLog("Unloading provider: ${provider.name}")
        safeCall("Exception while unloading provider: ${provider.name}") {
            provider.onUnload(context.applicationContext)

            providerDataList.removeIf {
                it.name.equals(provider.name, true)
            }
            classLoaders.values.removeIf { 
                it.name.equals(provider.name, true)
            }
            providerApiRepository.remove(provider.name)
            providers.remove(provider.name)
            if (unloadOnSettings) {
                unloadProviderOnSettings(
                    name = provider.name,
                    path = file.absolutePath
                )
            }
            file.delete()
            
            // Delete updater.json file if its the only thing remaining on that folder
            val parentFolder = file.parentFile!!
            if (parentFolder.isDirectory && parentFolder.listFiles()?.size == 1) {
                val lastRemainingFile = parentFolder.listFiles()!![0]
                
                if (lastRemainingFile.name.equals("updater.json", true)) {
                    rmrf(parentFolder)
                }
            }
        }
    }

    private suspend fun unloadProviderOnSettings(name: String, path: String) {
        dataStoreManager.updateUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY) {
            val newList = it.providers.toMutableList()
            newList.removeIf { providerPref ->
                providerPref.filePath == path
                && providerPref.name == name
            }

            it.copy(providers = newList)
        }
    }

    private suspend fun reloadProviderOnSettings(
        oldProviderData: ProviderData,
        newProviderData: ProviderData
    ): ProviderOrderEntity {
        val oldOrderPosition = getPositionIndexFromSettings(oldProviderData.name)
        val oldPreference = providerPreferences.providers[oldOrderPosition]

        val localPrefix = if (oldPreference.filePath.contains(LOCAL_PATH_PREFIX)) LOCAL_PATH_PREFIX else null
        val newPath = context.provideValidProviderPath(
            newProviderData,
            localPrefix = localPrefix
        )
        val newPreference = oldPreference.copy(
            name = newProviderData.name,
            filePath = newPath.absolutePath
        )

        loadProviderOnSettings(
            provider = newPreference,
            index = oldOrderPosition
        )
        
        return newPreference
    }

    suspend fun reloadProvider(
        oldProviderData: ProviderData,
        newProviderData: ProviderData
    ) {
        if (!providers.containsKey(oldProviderData.name))
            throw IllegalArgumentException("No such provider: ${oldProviderData.name}")

        unloadProvider(
            providerData = oldProviderData,
            unloadOnSettings = false
        )

        val newProviderPreference = reloadProviderOnSettings(
            oldProviderData = oldProviderData,
            newProviderData = newProviderData
        )

        loadProvider(
            providerData = newProviderData,
            filePath = newProviderPreference.filePath,
            needsDownload = true
        )
    }

    suspend fun swapProvidersOrder(
        fromIndex: Int,
        toIndex: Int
    ) {
        dataStoreManager.updateUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY) {
            val newProvidersOrder = it.providers.toMutableList()
            val size = newProvidersOrder.size

            if (fromIndex == toIndex || fromIndex < 0 || toIndex < 0 || fromIndex >= size || toIndex >= size) {
                return@updateUserPrefs it
            }

            val tempProviderPreference = newProvidersOrder[fromIndex]
            newProvidersOrder[fromIndex] = newProvidersOrder[toIndex]
            newProvidersOrder[toIndex] = tempProviderPreference

            val tempProviderData = providerDataList[fromIndex]
            providerDataList[fromIndex] = providerDataList[toIndex]
            providerDataList[toIndex] = tempProviderData

            it.copy(providers = newProvidersOrder)
        }
    }

    /**
     * Toggles a provider. If it is enabled, it will be disabled and vice versa.
     *
     * @param providerData The data of the provider to toggle
     */
    suspend fun toggleUsage(providerData: ProviderData) {
        val isProviderEnabled = isProviderEnabled(providerData.name)
        toggleUsageOnSettings(
            name = providerData.name,
            isDisabled = isProviderEnabled
        )

        if (isProviderEnabled) {
            providerApiRepository.remove(providerData.name)
        } else {
            try {
                val api = providers[providerData.name]
                    ?.getApi(context, client)

                if (api != null) {
                    providerApiRepository.add(
                        providerName = providerData.name,
                        providerApi = api
                    )
                }
            } catch (e: Throwable) {
                val message = context.getApiCrashMessage(provider = providerData.name)
                errorLog(message)
                context.showToast(message)

                toggleUsage(providerData)
            }
        }
    }

    private suspend fun toggleUsageOnSettings(
        name: String,
        isDisabled: Boolean
    ) {
        dataStoreManager.updateUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY) {
            withDefaultContext {
                val listOfSavedProviders = it.providers.toMutableList()

                val indexOfProvider = listOfSavedProviders.indexOfFirst { provider ->
                    provider.name.equals(name, true)
                }
                val provider = listOfSavedProviders[indexOfProvider]

                listOfSavedProviders[indexOfProvider] = provider.copy(isDisabled = isDisabled)

                it.copy(providers = listOfSavedProviders.toList())
            }
        }
    }

    private suspend fun getPositionIndexFromSettings(name: String): Int {
        return withDefaultContext {
            providerPreferences
                .providers
                .indexOfFirst { it.name.equals(name, true) }
        }
    }

    /**
     * Checks whether a provider is enabled
     *
     * @param name Name of the provider
     * @return Whether the provider is enabled
     */
    fun isProviderEnabled(name: String): Boolean {
        return providerPreferences.providers
            .find { it.name.contains(name, true) }
            ?.isDisabled?.not() ?: true
    }
}