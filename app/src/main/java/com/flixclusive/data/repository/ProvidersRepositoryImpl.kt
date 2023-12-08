package com.flixclusive.data.repository

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.flixclusive.domain.model.provider.SourceProviderDetails
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.domain.preferences.AppSettingsManager
import com.flixclusive.domain.repository.ProvidersRepository
import com.flixclusive.providers.interfaces.SourceProvider
import com.flixclusive.providers.sources.flixhq.FlixHQ
import com.flixclusive.providers.sources.lookmovie.LookMovie
import com.flixclusive.providers.sources.superstream.SuperStream
import okhttp3.OkHttpClient
import javax.inject.Inject

class ProvidersRepositoryImpl @Inject constructor(
    private val client: OkHttpClient,
    private val appSettingsManager: AppSettingsManager,
) : ProvidersRepository {
    private fun getSourceProvider(sourceName: String, client: OkHttpClient): SourceProvider {
        return when(sourceName) {
            "superstream" -> SuperStream(client)
            "lookmovie" -> LookMovie(client)
            "flixhq" -> FlixHQ(client)
            else -> throw Exception("Invalid source provider provided.")
        }
    }

    override val providers: SnapshotStateList<SourceProviderDetails> = mutableStateListOf()

    override fun populate(
        name: String,
        isIgnored: Boolean,
        isMaintenance: Boolean
    ) {
        if(providers.find { it.source.name == name } != null)
            return

        providers.add(
            SourceProviderDetails(
                source = getSourceProvider(name, client),
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
        
        // === SWAP on APP SETTINGS
        val providerConfig = appSettings.providers.toMutableList()
        val tempProvidersConfig = providerConfig[fromIndex]
        providerConfig[fromIndex] = providerConfig[toIndex]
        providerConfig[toIndex] = tempProvidersConfig

        appSettingsManager.updateData(
            appSettings.copy(
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


        // === TOGGLE USAGE on APP SETTINGS
        val providerConfig = appSettings.providers.toMutableList()
        val dataProvidersConfig = providerConfig[index]
        providerConfig[index] = dataProvidersConfig.copy(
            isIgnored = !dataProvidersConfig.isIgnored
        )

        appSettingsManager.updateData(
            appSettings.copy(
                providers = providerConfig
            )
        )
        // =================================
    }
}