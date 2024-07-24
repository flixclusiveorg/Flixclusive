package com.flixclusive.provider

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.Composable
import com.flixclusive.gradle.entities.ProviderManifest
import com.flixclusive.provider.settings.ProviderSettings
import okhttp3.OkHttpClient

/**
 *
 * The base class for all providers. Note that this differs from [ProviderApi], this holds every the detailed information of the provider.
 *
 * @property name The name of the provider.
 * @property manifest A [ProviderManifest] instance that contains the provider's information.
 * @property resources A [Resources] instance that is used to hold all of the app's resources.
 * @property __filename The filename of the provider.
 *
 * @property settings A [ProviderSettings] instance that holds the provider's settings/preferences.
 * */
@Suppress("PropertyName")
abstract class Provider {
    var manifest: ProviderManifest? = null
    val name: String? get() = manifest?.name
    var resources: Resources? = null
    var __filename: String? = null

    lateinit var settings: ProviderSettings

    /**
     * Called when the [Provider] is loaded. Should return a [ProviderApi] instance.
     *
     * @param context The app's context
     * @param client The app's global [OkHttpClient] for network requests
     */
    @Throws(Throwable::class)
    abstract fun getApi(
        context: Context?,
        client: OkHttpClient
    ): ProviderApi


    /**
     * Called before the [Provider] is unloaded
     * @param context Context
     */
    @Throws(Throwable::class)
    open fun onUnload(context: Context?) = Unit

    /**
     *
     * The custom settings screen composable to be displayed
     * when the user clicks the provider. Override this
     * to have a settings screen.
     *
     * #### To enhance code readability, always prefer to extract components by functions
     * ```
     * @Composable
     * override fun SettingsScreen() {
     *      // Create a custom component for code readability
     *      MyCustomSettingsScreen(resources)
     * }
     * ```
     * */
    @Composable
    open fun SettingsScreen() = Unit
}