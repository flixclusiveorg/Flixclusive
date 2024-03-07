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
import com.flixclusive.gradle.entities.PluginData
import com.flixclusive.gradle.entities.PluginManifest
import com.flixclusive.model.datastore.PluginPreference
import com.flixclusive.provider.plugin.Plugin
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
class PluginManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val appSettingsManager: AppSettingsManager,
    private val client: OkHttpClient,
    private val providersRepository: ProviderRepository,
) {
    /** Map containing all loaded plugins  */
    val plugins: MutableMap<String, Plugin> = LinkedHashMap()
    val classLoaders: MutableMap<PathClassLoader, Plugin> = HashMap()

    // An observable map of plugin data
    val pluginDataMap = mutableStateMapOf<String, PluginData>()

    /** Plugins that failed to load for various reasons. Map of file to String or Exception  */
    val failedToLoad: MutableMap<File, Any> = LinkedHashMap()

    fun initialize() {
        scope.launch {
            val appSettings = appSettingsManager.localAppSettings

            val updaterJsonMap = HashMap<String, List<PluginData>>()

            appSettings.plugins.forEach { pluginPreference ->
                val pluginFile = File(pluginPreference.filePath)
                val updaterJsonFilePath = pluginFile.parent?.plus("/updater.json")

                if (updaterJsonFilePath == null) {
                    errorLog("Plugin's file path must not be null!")
                    return@launch
                }

                val updaterJsonFile = File(updaterJsonFilePath)


                if (!updaterJsonFile.exists()) {
                    errorLog("Plugin's updater.json could not be found!")
                    return@forEach
                }

                val updaterJsonList = updaterJsonMap.getOrPut(updaterJsonFile.absolutePath) {
                    fromJson<List<PluginData>>(updaterJsonFile.reader())
                }

                val pluginName = pluginFile.name
                val pluginData = updaterJsonList.find { it.name.equals(pluginName, true) }

                if (pluginData == null) {
                    errorLog("Plugin cannot be found on the updater.json file!")
                    return@launch
                }

                if (pluginName.endsWith(".flx")) {
                    loadPlugin(
                        file = pluginFile,
                        pluginData = pluginData
                    )
                } else if (pluginName != "oat") { // Some roms create this
                    if (pluginFile.isDirectory) {
                        context.showToast(
                            String.format(context.getString(UtilR.string.invalid_plugin_file_directory_msg_format), pluginName)
                        )
                    } else if (pluginName.equals("classes.dex") || pluginName.endsWith(".json")) {
                        context.showToast(
                            String.format(context.getString(UtilR.string.invalid_plugin_file_dex_json_msg_format), pluginName)
                        )
                    }
                    rmrf(pluginFile)
                }
            }

            if (failedToLoad.isNotEmpty())
                context.showToast(
                    message = context.getString(UtilR.string.failed_to_load_plugins_msg),
                    duration = Toast.LENGTH_LONG
                )
        }
    }

    /**
     * Loads a plugin
     *
     * @param client a modified [OkHttpClient] instance
     * @param file   Plugin file
     */
    suspend fun loadPlugin(file: File, pluginData: PluginData) {
        val fileName = file.nameWithoutExtension
        val filePath = file.absolutePath

        val pluginPreference = getPluginPreference(fileName)

        infoLog("Loading plugin: $fileName")

        safeCall {
            File(filePath).setReadOnly()
        }

        try {
            pluginDataMap[fileName] = pluginData

            val loader = PathClassLoader(filePath, context.classLoader)
            var manifest: PluginManifest
            
            loader.getResourceAsStream("manifest.json").use { stream ->
                if (stream == null) {
                    failedToLoad[file] = "No manifest found"
                    errorLog("Failed to load plugin $fileName: No manifest found")
                    return
                }
                InputStreamReader(stream).use { reader ->
                    manifest = fromJson(reader)
                }
            }
            val name = manifest.name
            val pluginClass: Class<out Plugin?> =
                loader.loadClass(manifest.pluginClassName) as Class<out Plugin>
            val pluginInstance: Plugin =
                pluginClass.getDeclaredConstructor().newInstance() as Plugin

            if (plugins.containsKey(name)) {
                errorLog("Plugin with name $name already exists")
                return
            }

            pluginInstance.__filename = fileName
            if (manifest.requiresResources) {
                // based on https://stackoverflow.com/questions/7483568/dynamic-resource-loading-from-other-apk
                val assets = AssetManager::class.java.getDeclaredConstructor().newInstance()
                val addAssetPath =
                    AssetManager::class.java.getMethod("addAssetPath", String::class.java)
                addAssetPath.invoke(assets, file.absolutePath)
                pluginInstance.resources = Resources(
                    assets,
                    context.resources.displayMetrics,
                    context.resources.configuration
                )
            }
            plugins[name] = pluginInstance
            classLoaders[loader] = pluginInstance

            pluginInstance.load(context, client)

            if (pluginPreference == null) {
                loadPluginOnSettings(
                    PluginPreference(
                        name = file.nameWithoutExtension,
                        filePath = filePath
                    )
                )
            }
        } catch (e: Throwable) {
            failedToLoad[file] = e
            errorLog("Failed to load plugin $fileName:\n")
        }
    }

    private suspend fun loadPluginOnSettings(pluginPreference: PluginPreference) {
        val appSettings = appSettingsManager.localAppSettings

        appSettingsManager.updateData(
            appSettings.copy(
                plugins = appSettings.plugins + listOf(pluginPreference)
            )
        )
    }

    /**
     * Unloads a plugin
     *
     * @param name Name of the plugin to unload
     */
    fun unloadPlugin(name: String) {
        infoLog("Unloading plugin: $name")
        val plugin = plugins[name]
        if (plugin != null) try {
            plugin.onUnload(context.applicationContext)

            pluginDataMap.remove(plugin.getName())
            classLoaders.values.removeIf { it.getName().equals(plugin.getName(), true) }
            providersRepository.remove(plugin.__filename!!)
            plugins.remove(name)
        } catch (e: Throwable) {
            errorLog("Exception while unloading plugin: $name")
        }
    }

    private suspend fun unloadPluginOnSettings(pluginPreference: PluginPreference) {
        val appSettings = appSettingsManager.localAppSettings

        appSettingsManager.updateData(
            appSettings.copy(
                plugins = appSettings.plugins + listOf(pluginPreference)
            )
        )
    }

    suspend fun swap(
        fromIndex: Int,
        toIndex: Int
    ) {
        val appSettings = appSettingsManager.localAppSettings
        appSettings.plugins.run {
            val size = size
            val newPluginsOrder = toMutableList()

            if (fromIndex == toIndex || fromIndex < 0 || toIndex < 0 || fromIndex >= size || toIndex >= size) {
                return
            }

            val tempProvidersList = newPluginsOrder[fromIndex]
            newPluginsOrder[fromIndex] = newPluginsOrder[toIndex]
            newPluginsOrder[toIndex] = tempProvidersList

            appSettingsManager.updateData(
                appSettings.copy(plugins = newPluginsOrder)
            )
        }
    }

    /**
     * Enables a loaded plugin if it isn't already enabled
     *
     * @param name Name of the plugin to enable
     */
    suspend fun enablePlugin(name: String) {
        if (isPluginEnabled(name)) return

        toggleUsage(name = name, isDisabled = false)
    }

    /**
     * Disables a loaded plugin if it isn't already disables
     *
     * @param name Name of the plugin to disable
     */
    suspend fun disablePlugin(name: String) {
        if (!isPluginEnabled(name)) return

        toggleUsage(name = name, isDisabled = true)
    }

    /**
     * Toggles a plugin. If it is enabled, it will be disabled and vice versa.
     *
     * @param name Name of the plugin to toggle
     */
    suspend fun toggleUsage(name: String) {
        if (isPluginEnabled(name)) disablePlugin(name) else enablePlugin(name)
    }

    /**
     * Toggles a plugin. If it is enabled, it will be disabled and vice versa.
     *
     * @param index Index of the plugin
     */
    suspend fun toggleUsage(index: Int) {
        val name = plugins.values.toList().getOrNull(index)?.getName() ?: return

        toggleUsage(name)
    }

    private suspend fun toggleUsage(
        name: String,
        isDisabled: Boolean
    ) {
        val appSettings = appSettingsManager.localAppSettings
        val listOfSavedProviders = appSettings.plugins.toMutableList()

        val indexOfPlugin = listOfSavedProviders.indexOfFirst { it.name.equals(name, true) }
        val plugin = listOfSavedProviders[indexOfPlugin]

        listOfSavedProviders[indexOfPlugin] = plugin.copy(isDisabled = isDisabled)

        appSettingsManager.updateData(
            appSettings.copy(
                plugins = listOfSavedProviders.toList()
            )
        )
    }

    fun getPluginPreference(name: String): PluginPreference? {
        return appSettingsManager.localAppSettings
            .plugins
            .find { it.name.equals(name, true) }
    }

    /**
     * Checks whether a plugin is enabled
     *
     * @param name Name of the plugin
     * @return Whether the plugin is enabled
     */
    fun isPluginEnabled(name: String): Boolean {
        return appSettingsManager
            .localAppSettings
            .plugins
            .find { it.name.contains(name, true) }
            ?.isDisabled?.not() ?: false
    }

    /**
     * Checks whether a plugin is enabled
     *
     * @param plugin Plugin
     * @return Whether the plugin is enabled
     */
    fun isPluginEnabled(plugin: Plugin): Boolean {
        return isPluginEnabled(plugin.getName() ?: return false)
    }
}