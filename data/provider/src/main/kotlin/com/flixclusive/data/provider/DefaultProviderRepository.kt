package com.flixclusive.data.provider

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.provider.base.ProviderData
import com.flixclusive.provider.flixhq.FlixHQ
import com.flixclusive.provider.lookmovie.LookMovie
import com.flixclusive.provider.superstream.SuperStream
import okhttp3.OkHttpClient
import javax.inject.Inject

internal class DefaultProviderRepository @Inject constructor(
    client: OkHttpClient,
    private val appSettingsManager: AppSettingsManager,
) : ProviderRepository {
    private val listOfAvailableProviders = arrayListOf(
        SuperStream(client),
        LookMovie(client),
        FlixHQ(client)
    )

    override val providers: SnapshotStateList<ProviderData> = mutableStateListOf()

    override fun populate(
        name: String,
        isIgnored: Boolean,
        isMaintenance: Boolean
    ) {
        if(providers.any { it.provider.name == name })
            return

        val provider = listOfAvailableProviders.find { it.name.equals(name, true) }
            ?: throw Exception("Can't find this provider in the available list.")

        providers.add(
            ProviderData(
                provider = provider,
                isMaintenance = isMaintenance,
                isIgnored = isIgnored
            )
        )
    }

    override suspend fun swap(
        appSettings: AppSettings,
        fromIndex: Int,
        toIndex: Int
    ) {
        val size = providers.size
        if (fromIndex == toIndex || fromIndex < 0 || toIndex < 0 || fromIndex >= size || toIndex >= size) {
            return
        }

        val tempProvidersList = providers[fromIndex]
        providers[fromIndex] = providers[toIndex]
        providers[toIndex] = tempProvidersList
        
        appSettings.swapProviders(
            fromIndex = fromIndex,
            toIndex = toIndex
        )
    }

    /**
     *
     * Swap the provider in app settings
     *
     * */
    private suspend fun AppSettings.swapProviders(
        fromIndex: Int,
        toIndex: Int
    ) {
        // === SWAP on APP SETTINGS
        val providerConfig = providers.toMutableList()
        val tempProvidersConfig = providerConfig[fromIndex]
        providerConfig[fromIndex] = providerConfig[toIndex]
        providerConfig[toIndex] = tempProvidersConfig

        appSettingsManager.updateData(
            copy(
                providers = providerConfig
            )
        )
        // ========================
    }

    override suspend fun toggleUsage(
        appSettings: AppSettings,
        index: Int
    ) {
        val size = providers.size
        if (index >= size) {
            return
        }

        val dataProvidersList = providers[index]
        providers[index] = dataProvidersList.copy(
            isIgnored = !dataProvidersList.isIgnored
        )


        appSettings.toggleProvider(index)
    }


    /**
     *
     * Toggles whether to use the provider
     * inside app settings.
     *
     * */
    private suspend fun AppSettings.toggleProvider(index: Int) {
        // === TOGGLE USAGE on APP SETTINGS
        val providerConfig = providers.toMutableList()
        val dataProvidersConfig = providerConfig[index]
        providerConfig[index] = dataProvidersConfig.copy(
            isIgnored = !dataProvidersConfig.isIgnored
        )

        appSettingsManager.updateData(
            copy(
                providers = providerConfig
            )
        )
        // =================================
    }
}