package com.flixclusive.provider

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.Composable
import com.flixclusive.gradle.entities.ProviderManifest
import com.flixclusive.provider.settings.ProviderSettingsManager
import okhttp3.OkHttpClient

@Suppress("PropertyName")
abstract class Provider() {
    lateinit var settings: ProviderSettingsManager
    private var manifest: ProviderManifest? = null

    constructor(manifest: ProviderManifest) : this() {
        this.manifest = manifest
        settings = ProviderSettingsManager(manifest.name)
    }

    /**
     * Resources associated with the provider, if specified.
     * */
    var resources: Resources? = null
    /**
     * The filename of the provider.
     * */
    var __filename: String? = null

    /**
     * Called when the [Provider] is loaded
     *
     * @param context The app's context
     * @param client The app's global [OkHttpClient] for network requests
     */
    @Throws(Throwable::class)
    open fun getApi(
        context: Context?,
        client: OkHttpClient
    ): ProviderApi {
        TODO("Return a ProviderApi here")
    }


    /**
     * Called before the [Provider] is unloaded
     * @param context Context
     */
    @Throws(Throwable::class)
    open fun onUnload(context: Context?) = Unit

    /**
     *
     * Get the provider's name
     *
     * @return the provider's name
     * */
    open fun getName(): String? {
        return manifest?.name
    }

    /**
     *
     * The custom settings screen composable to be displayed
     * when the user clicks the provider. Override this
     * to have a settings screen.
     *
     * #### To enhance code readability, always prefer to extract components by functions
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
    ) = Unit
}