package com.flixclusive.data.repository

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.flixclusive.domain.model.provider.SourceProviderDetails
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.domain.preferences.AppSettingsManager
import com.flixclusive.domain.repository.ProvidersRepository
import com.flixclusive.providers.sources.flixhq.FlixHQ
import com.flixclusive.providers.sources.lookmovie.LookMovie
import com.flixclusive.providers.sources.superstream.SuperStream
import okhttp3.OkHttpClient
import javax.inject.Inject

class ProvidersRepositoryImpl @Inject constructor(
    client: OkHttpClient,
    private val appSettingsManager: AppSettingsManager,
) : ProvidersRepository {
    private val listOfAvailableProviders = arrayListOf(
        SuperStream(client),
        LookMovie(client),
        FlixHQ(client)
    )

    override val providers: SnapshotStateList<SourceProviderDetails> = mutableStateListOf()

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
            SourceProviderDetails(
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