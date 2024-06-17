package com.flixclusive.provider

import android.content.Context
import android.content.res.Resources
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flixclusive.core.util.R as UtilR
import com.flixclusive.gradle.entities.ProviderManifest
import com.flixclusive.provider.settings.ProviderSettingsManager
import okhttp3.OkHttpClient

@Suppress("PropertyName")
abstract class Provider {
    lateinit var settings: ProviderSettingsManager
    var manifest: ProviderManifest? = null

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
     * override fun SettingsScreen() {
     *      // Create a custom component for code readability
     *      MyCustomSettingsScreen(resources)
     * }
     * ```
     * */
    @Composable
    open fun SettingsScreen() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(UtilR.string.non_configurable_provider_message),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }
    }
}