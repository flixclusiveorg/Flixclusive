package com.flixclusive.provider.plugin

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.Composable
import com.flixclusive.gradle.entities.PluginManifest
import com.flixclusive.provider.plugin.settings.PluginSettingsManager
import okhttp3.OkHttpClient


/**
 * Represents a plugin for a provider.
 *
 */
@Suppress("PropertyName")
abstract class Plugin() {
    lateinit var settings: PluginSettingsManager
    private var manifest: PluginManifest? = null

    constructor(manifest: PluginManifest) : this() {
        this.manifest = manifest
        settings = PluginSettingsManager(manifest.name)
    }

    /**
     * Resources associated with the plugin, if specified.
     * */
    var resources: Resources? = null
    /**
     * The filename of the plugin.
     * */
    var __filename: String? = null

    /**
     * Called when your Plugin is loaded
     * @param context The app's context
     * @param client The app's global [OkHttpClient] for network requests
     */
    @Throws(Throwable::class)
    open fun load(
        context: Context?,
        client: OkHttpClient
    ) {
        // TODO(Add default value)
    }


    /**
     * Called before your Plugin is unloaded
     * @param context Context
     */
    @Throws(Throwable::class)
    open fun onUnload(context: Context?) {
        // TODO(Add default value)
    }

    /**
     *
     * Get the plugin's name
     *
     * @return the plugin's name
     * */
    open fun getName(): String? {
        return manifest?.name
    }

    /**
     *
     * The custom settings screen composable to be displayed
     * when the user clicks the plugin. Override this if you
     * wish to have your own settings screen.
     *
     * To enhance code readability, always prefer to extract
     * components by functions
     *
     * ### Sample:
     * ```
     * @Composable
     * override fun SettingsScreen(
     *      resources: Resources? = this.resources
     * ) {
     *      // Create a custom component for code readability
     *      MyCustomSettingsScreen(resources)
     * }
     * ```
     * */
    @Composable
    open fun SettingsScreen(
        resources: Resources? = this.resources,
    ) {
        // TODO(Add default value)
    }
}