package com.flixclusive.data.provider

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.ui.common.util.showToast
import com.flixclusive.core.util.common.dispatcher.AppDispatchers
import com.flixclusive.core.util.common.dispatcher.Dispatcher
import com.flixclusive.core.util.common.dispatcher.di.ApplicationScope
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.data.provider.util.buildValidFilename
import com.flixclusive.data.provider.util.downloadFile
import com.flixclusive.data.provider.util.provideValidProviderPath
import com.flixclusive.data.provider.util.replaceLastAfterSlash
import com.flixclusive.data.provider.util.rmrf
import com.flixclusive.gradle.entities.ProviderData
import com.flixclusive.gradle.entities.ProviderManifest
import com.flixclusive.gradle.entities.Repository.Companion.toValidRepositoryLink
import com.flixclusive.model.datastore.provider.ProviderPreference
import com.flixclusive.provider.Provider
import com.flixclusive.provider.settings.ProviderSettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dalvik.system.PathClassLoader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton
import com.flixclusive.core.util.R as UtilR

const val PROVIDERS_FOLDER = "flx_providers"

@Singleton
class ProviderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    private val appSettingsManager: AppSettingsManager,
    private val client: OkHttpClient,
    private val providersRepository: ProviderRepository,
) {
    /** Map containing all loaded providers  */
    val providers: MutableMap<String, Provider> = LinkedHashMap()
    val classLoaders: MutableMap<PathClassLoader, Provider> = HashMap()

    // An observable map of provider data
    val providerDataList = mutableStateListOf<ProviderData>()

    /** Providers that failed to load for various reasons. Map of file to String or Exception  */
    val failedToLoad: MutableMap<File, Any> = LinkedHashMap()

    /**
     *
     * Map of all repository's updater.json files and their contents
     * */
    private val updaterJsonMap = HashMap<String, List<ProviderData>>()

    fun initialize() {
        scope.launch {
            initializeLocalProviders()

            val providerSettings = appSettingsManager.providerSettings.data.first()

            providerSettings.providers.forEach { providerPreference ->
                val file = File(providerPreference.filePath)

                if (!file.exists()) {
                    warnLog("Provider file doesn't exist for: ${providerPreference.name}")
                    return@forEach
                }

                initializeProvider(providerFile = file)
            }

            if (failedToLoad.isNotEmpty())
                context.showToast(
                    message = context.getString(UtilR.string.failed_to_load_providers_msg),
                    duration = Toast.LENGTH_LONG
                )
        }
    }

    private suspend fun initializeLocalProviders() {
        val localPath = context.getExternalFilesDir(null)?.absolutePath + "/providers/"
        val localDir = File(localPath)

        if (!localDir.exists()) {
            val isSuccess = localDir.mkdirs()
            if (!isSuccess) {
                warnLog("Failed to create local directories when loading providers.")
            }

            return
        }

        val providerSettings = appSettingsManager.providerSettings.data.first()
        val repositoryFolders = localDir.listFiles()

        repositoryFolders?.mapAsync folderMap@ { folder ->
            if (!folder.isDirectory)
                return@folderMap

            val updaterJsonFile = File(folder.absolutePath + "/updater.json")
            if (!updaterJsonFile.exists()) {
                errorLog("Provider's updater.json could not be found!")
                return@folderMap
            }

            val randomProviderFile = fromJson<List<ProviderData>>(updaterJsonFile.reader())
                .firstOrNull()
                ?: return@folderMap

            val repository = randomProviderFile.repositoryUrl?.toValidRepositoryLink()
                ?: return@folderMap

            if (!providerSettings.repositories.contains(repository)) {
                appSettingsManager.updateProviderSettings {
                    it.copy(repositories = it.repositories + repository)
                }
            }

            folder.listFiles()
                ?.mapAsync { providerFile ->
                    val isProviderNotYetLoaded
                        = providerSettings.providers.any {
                            it.filePath == providerFile.absolutePath
                        }.not()

                    if (isProviderNotYetLoaded) {
                        appSettingsManager.updateProviderSettings {
                            it.copy(
                                providers =
                                    it.providers + ProviderPreference(
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
        val updaterJsonFilePath = providerFile.parent?.plus("/updater.json")

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
            errorLog("Provider cannot be found on the updater.json file!")
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
                    String.format(context.getString(UtilR.string.invalid_provider_file_directory_msg_format), providerName)
                )
            } else if (providerName.equals("classes.dex") || providerName.endsWith(".json")) {
                context.showToast(
                    String.format(context.getString(UtilR.string.invalid_provider_file_dex_json_msg_format), providerName)
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
        val updaterJsonFile = File(file.parent!!.plus("/updater.json"))

        // Download provider
        val isProviderDownloadSuccess = withContext(ioDispatcher) {
            client.downloadFile(
                file = file, downloadUrl = buildUrl
            )
        }

        // Download updater.json
        val isUpdaterJsonDownloadSuccess = withContext(ioDispatcher) {
            client.downloadFile(
                file = updaterJsonFile, downloadUrl = updaterJsonUrl
            )
        }

        return isProviderDownloadSuccess && isUpdaterJsonDownloadSuccess
    }

    suspend fun loadProvider(
        providerData: ProviderData,
        needsDownload: Boolean = false
    ) {
        check(providerData.repositoryUrl != null) {
            "Repository URL must not be null if using this overloaded method."
        }

        val file = context.provideValidProviderPath(providerData)

        if (needsDownload && !downloadProvider(file, providerData.buildUrl!!)) {
            throw IOException("Something went wrong trying to download the provider.")
        }

        loadProvider(
            file = file,
            providerData = providerData
        )
    }

    /**
     * Loads a provider
     *
     * @param file          Provider file
     * @param providerData    The provider information
     */
    private suspend fun loadProvider(file: File, providerData: ProviderData) {
        val fileName = file.nameWithoutExtension
        val filePath = file.absolutePath

        val providerPreference = getProviderPreference(fileName)

        infoLog("Loading provider: $fileName")

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
            val name = manifest.name
            val providerClass: Class<out Provider?> =
                loader.loadClass(manifest.providerClassName) as Class<out Provider>
            val providerInstance: Provider =
                providerClass.getDeclaredConstructor().newInstance() as Provider

            if (providers.containsKey(name)) {
                errorLog("Provider with name $name already exists")
                return
            }

            providerDataList.add(providerData)

            val settingsPath = context.getExternalFilesDir(null)
                ?.absolutePath + "/settings/${buildValidFilename(providerData.repositoryUrl!!)}"

            providerInstance.__filename = fileName
            providerInstance.manifest = manifest
            providerInstance.settings = ProviderSettingsManager(
                settingsPath = settingsPath,
                providerName = providerData.name
            )
            if (manifest.requiresResources) {
                // based on https://stackoverflow.com/questions/7483568/dynamic-resource-loading-from-other-apk
                val assets = AssetManager::class.java.getDeclaredConstructor().newInstance()
                val addAssetPath =
                    AssetManager::class.java.getMethod("addAssetPath", String::class.java)
                addAssetPath.invoke(assets, file.absolutePath)
                providerInstance.resources = Resources(
                    assets,
                    context.resources.displayMetrics,
                    context.resources.configuration
                )
            }
            providers[name] = providerInstance
            classLoaders[loader] = providerInstance

            val api = providerInstance.getApi(context, client)
            providersRepository.add(
                providerName = providerData.name,
                providerApi = api
            )

            if (providerPreference == null) {
                loadProviderOnSettings(
                    ProviderPreference(
                        name = fileName,
                        filePath = filePath,
                        isDisabled = false
                    )
                )
            }
        } catch (e: Throwable) {
            failedToLoad[file] = e
            errorLog("Failed to load provider $fileName: ${e.localizedMessage}")
            throw e
        }
    }

    private suspend fun loadProviderOnSettings(providerPreference: ProviderPreference) {
        appSettingsManager.updateProviderSettings {
            it.copy(
                providers = it.providers + listOf(providerPreference)
            )
        }
    }

    /**
     * Unloads a provider
     *
     * @param providerData the [ProviderData] to uninstall/unload
     */
    suspend fun unloadProvider(providerData: ProviderData) {
        val provider = providers[providerData.name]
        val file = context.provideValidProviderPath(providerData)

        if (provider != null && file.exists()) {
            unloadProvider(provider, file)
        }
    }

    /**
     *
     * Unloads a provider based on [ProviderPreference]
     *
     * @param providerPreference the [ProviderPreference] required that contains the file path and provider's name
     * */
    suspend fun unloadProvider(providerPreference: ProviderPreference) {
        val provider = providers[providerPreference.name]
        val file = File(providerPreference.filePath)

        if (provider != null && file.exists()) {
            unloadProvider(provider, file)
        }
    }

    private suspend fun unloadProvider(
        provider: Provider,
        file: File
    ) {
        infoLog("Unloading provider: ${provider.getName()}")
        safeCall("Exception while unloading provider: ${provider.getName()}") {
            provider.onUnload(context.applicationContext)

            providerDataList.removeIf {
                it.name.equals(file.nameWithoutExtension, true)
            }
            classLoaders.values.removeIf { it.getName().equals(provider.getName(), true) }
            providersRepository.remove(provider.getName()!!)
            providers.remove(provider.getName())
            unloadProviderOnSettings(file.absolutePath)
            file.delete()
        }
    }

    private suspend fun unloadProviderOnSettings(path: String) {
        appSettingsManager.updateProviderSettings {
            val newList = it.providers.toMutableList()
            newList.removeIf { providerPref ->
                providerPref.equals(path)
            }

            it.copy(providers = newList)
        }
    }

    suspend fun swap(
        fromIndex: Int,
        toIndex: Int
    ) {
        appSettingsManager.updateProviderSettings {
            val newProvidersOrder = it.providers.toMutableList()
            val size = newProvidersOrder.size

            if (fromIndex == toIndex || fromIndex < 0 || toIndex < 0 || fromIndex >= size || toIndex >= size) {
                return@updateProviderSettings it
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
     * @param name Name of the provider to toggle
     */
    suspend fun toggleUsage(providerData: ProviderData) {
        toggleUsageOnSettings(
            name = providerData.name,
            isDisabled = isProviderEnabled(providerData.name)
        )
    }

    private suspend fun toggleUsageOnSettings(
        name: String,
        isDisabled: Boolean
    ) {
        appSettingsManager.updateProviderSettings {
            val listOfSavedProviders = it.providers.toMutableList()

            val indexOfProvider = listOfSavedProviders.indexOfFirst { provider ->
                provider.name.equals(name, true)
            }
            val provider = listOfSavedProviders[indexOfProvider]

            listOfSavedProviders[indexOfProvider] = provider.copy(isDisabled = isDisabled)

            it.copy(providers = listOfSavedProviders.toList())
        }
    }

    private fun getProviderPreference(name: String): ProviderPreference? {
        return appSettingsManager.localProviderSettings
            .providers
            .find { it.name.equals(name, true) }
    }

    /**
     * Checks whether a provider is enabled
     *
     * @param name Name of the provider
     * @return Whether the provider is enabled
     */
    fun isProviderEnabled(name: String): Boolean {
        return appSettingsManager
            .localProviderSettings
            .providers
            .find { it.name.contains(name, true) }
            ?.isDisabled?.not() ?: false
    }
}