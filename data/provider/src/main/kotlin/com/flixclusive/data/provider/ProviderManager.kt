package com.flixclusive.data.provider

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.widget.Toast
import androidx.compose.runtime.mutableStateMapOf
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.ui.common.util.showToast
import com.flixclusive.core.util.common.dispatcher.di.ApplicationScope
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.data.provider.util.rmrf
import com.flixclusive.gradle.entities.ProviderData
import com.flixclusive.gradle.entities.ProviderManifest
import com.flixclusive.model.datastore.provider.ProviderPreference
import com.flixclusive.provider.Provider
import dagger.hilt.android.qualifiers.ApplicationContext
import dalvik.system.PathClassLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton
import com.flixclusive.core.util.R as UtilR


@Singleton
class ProviderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val appSettingsManager: AppSettingsManager,
    private val client: OkHttpClient,
    private val providersRepository: ProviderRepository,
) {
    /** Map containing all loaded providers  */
    val providers: MutableMap<String, Provider> = LinkedHashMap()
    val classLoaders: MutableMap<PathClassLoader, Provider> = HashMap()

    // An observable map of provider data
    val providerDataMap = mutableStateMapOf<String, ProviderData>()

    /** Providers that failed to load for various reasons. Map of file to String or Exception  */
    val failedToLoad: MutableMap<File, Any> = LinkedHashMap()

    fun initialize() {
        scope.launch {
            val providerSettings = appSettingsManager.localProviderSettings

            val updaterJsonMap = HashMap<String, List<ProviderData>>()

            providerSettings.providers.forEach { providerPreference ->
                val providerFile = File(providerPreference.filePath)
                val updaterJsonFilePath = providerFile.parent?.plus("/updater.json")

                if (updaterJsonFilePath == null) {
                    errorLog("Provider's file path must not be null!")
                    return@launch
                }

                val updaterJsonFile = File(updaterJsonFilePath)


                if (!updaterJsonFile.exists()) {
                    errorLog("Provider's updater.json could not be found!")
                    return@forEach
                }

                val updaterJsonList = updaterJsonMap.getOrPut(updaterJsonFile.absolutePath) {
                    fromJson<List<ProviderData>>(updaterJsonFile.reader())
                }

                val providerName = providerFile.name
                val providerData = updaterJsonList.find { it.name.equals(providerName, true) }

                if (providerData == null) {
                    errorLog("Provider cannot be found on the updater.json file!")
                    return@launch
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

            if (failedToLoad.isNotEmpty())
                context.showToast(
                    message = context.getString(UtilR.string.failed_to_load_providers_msg),
                    duration = Toast.LENGTH_LONG
                )
        }
    }

    /**
     * Loads a provider
     *
     * @param file          Provider file
     * @param providerData    The provider information
     */
    suspend fun loadProvider(file: File, providerData: ProviderData) {
        val fileName = file.nameWithoutExtension
        val filePath = file.absolutePath

        val providerPreference = getProviderPreference(fileName)

        infoLog("Loading provider: $fileName")

        safeCall {
            File(filePath).setReadOnly()
        }

        try {
            providerDataMap[fileName] = providerData

            val loader = PathClassLoader(filePath, context.classLoader)
            var manifest: ProviderManifest
            
            loader.getResourceAsStream("manifest.json").use { stream ->
                if (stream == null) {
                    failedToLoad[file] = "No manifest found"
                    errorLog("Failed to load provider $fileName: No manifest found")
                    return
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

            providerInstance.__filename = fileName
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

            providerInstance.getApi(context, client)

            if (providerPreference == null) {
                loadProviderOnSettings(
                    ProviderPreference(
                        name = fileName,
                        filePath = filePath
                    )
                )
            }
        } catch (e: Throwable) {
            failedToLoad[file] = e
            errorLog("Failed to load provider $fileName:\n")
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
     * @param name Name of the provider to unload
     */
    suspend fun unloadProvider(name: String) {
        infoLog("Unloading provider: $name")
        val provider = providers[name]
        if (provider != null) {
            safeCall("Exception while unloading provider: $name") {
                provider.onUnload(context.applicationContext)

                providerDataMap.remove(provider.getName())
                classLoaders.values.removeIf { it.getName().equals(provider.getName(), true) }
                providersRepository.remove(provider.__filename!!)
                providers.remove(name)
                unloadProviderOnSettings(provider)
            }
        }
    }

    private suspend fun unloadProviderOnSettings(provider: Provider) {
        appSettingsManager.updateProviderSettings {
            val newList = it.providers.toMutableList()
            newList.removeIf { providerPref ->
                providerPref.equals(provider.__filename)
            }

            it.copy(
                providers = newList
            )
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

            val tempProvidersList = newProvidersOrder[fromIndex]
            newProvidersOrder[fromIndex] = newProvidersOrder[toIndex]
            newProvidersOrder[toIndex] = tempProvidersList

            it.copy(
                providers = newProvidersOrder
            )
        }
    }

    /**
     * Enables a loaded provider if it isn't already enabled
     *
     * @param name Name of the provider to enable
     */
    suspend fun enableProvider(name: String) {
        if (isProviderEnabled(name)) return

        toggleUsage(name = name, isDisabled = false)
    }

    /**
     * Disables a loaded provider if it isn't already disables
     *
     * @param name Name of the provider to disable
     */
    suspend fun disableProvider(name: String) {
        if (!isProviderEnabled(name)) return

        toggleUsage(name = name, isDisabled = true)
    }

    /**
     * Toggles a provider. If it is enabled, it will be disabled and vice versa.
     *
     * @param name Name of the provider to toggle
     */
    suspend fun toggleUsage(name: String) {
        if (isProviderEnabled(name)) disableProvider(name) else enableProvider(name)
    }

    /**
     * Toggles a provider. If it is enabled, it will be disabled and vice versa.
     *
     * @param index Index of the provider
     */
    suspend fun toggleUsage(index: Int) {
        val name = providers.values.toList().getOrNull(index)?.getName() ?: return

        toggleUsage(name)
    }

    private suspend fun toggleUsage(
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

            it.copy(
                providers = listOfSavedProviders.toList()
            )
        }
    }

    fun getProviderPreference(name: String): ProviderPreference? {
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

    /**
     * Checks whether a provider is enabled
     *
     * @param provider Provider
     * @return Whether the provider is enabled
     */
    fun isProviderEnabled(provider: Provider): Boolean {
        return isProviderEnabled(provider.getName() ?: return false)
    }
}