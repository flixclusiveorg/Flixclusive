package com.flixclusive.provider.base.plugin

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.Composable
import com.flixclusive.provider.base.Provider


/**
 * Represents a plugin for a provider.
 *
 * @property manifest The manifest information of the plugin.
 * @property SettingsScreen A composable function for opening custom settings, if provided.
 */
@Suppress("PropertyName")
abstract class Plugin(var manifest: PluginManifest? = null) {
    /**
     *
     * The list of [Provider]s to be loaded on the
     * providers list.
     * */
    abstract val providers: List<Provider>

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
     * @param context Context
     */
    @Throws(Throwable::class)
    open fun load(context: Context?) {
        // TODO(Add default value)
    }


    /**
     * Called when your Plugin is unloaded
     * @param context Context
     */
    @Throws(Throwable::class)
    open fun unload(context: Context?) {
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
        resources: Resources? = this.resources
    ) {}
}