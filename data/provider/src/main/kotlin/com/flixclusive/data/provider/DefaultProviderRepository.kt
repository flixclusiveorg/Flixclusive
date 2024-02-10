package com.flixclusive.data.provider

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.util.common.dispatcher.di.ApplicationScope
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.datastore.ProviderPreference
import com.flixclusive.provider.base.ProviderData
import com.flixclusive.provider.flixhq.FlixHQ
import com.flixclusive.provider.lookmovie.LookMovie
import com.flixclusive.provider.superstream.SuperStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

internal class DefaultProviderRepository @Inject constructor(
    client: OkHttpClient,
    @ApplicationScope private val scope: CoroutineScope,
    private val appSettingsManager: AppSettingsManager,
) : ProviderRepository {

    override val providers: SnapshotStateList<ProviderData> = mutableStateListOf()

    /**
     *
     * This is the list of currently embedded providers.
     * This will be removed soon to support provider add-ons/plugins.
     *
     * This is arranged by competence.
     * */
    private val listOfAvailableProviders = arrayListOf(
        FlixHQ(client),
        LookMovie(client),
        SuperStream(client)
    )

    /**
     *
     * Initializes provider. It could be based off [listOfAvailableProviders]
     * or [AppSettings.providers], if the user has a pre-defined order of providers.
     * */
    override fun initialize() {
        scope.launch {
            val appSettings = appSettingsManager.localAppSettings
            val providersPreferences = appSettings.providers.toMutableList()

            var isConfigEmpty = providersPreferences.isEmpty()
            val isNotInitializedCorrectly = providersPreferences.size < listOfAvailableProviders.size

            if (!isConfigEmpty && isNotInitializedCorrectly) {
                providersPreferences.clear()
                isConfigEmpty = true
            }

            listOfAvailableProviders.forEachIndexed { index, availableProvider ->
                val provider = if (isConfigEmpty) {
                    availableProvider
                } else {
                    listOfAvailableProviders.find {
                        it.name.equals(
                            other = providersPreferences[index].name,
                            ignoreCase = true
                        )
                    }!!
                }

                val isIgnored = providersPreferences.getOrNull(index)?.isIgnored ?: false

                providers.add(
                    ProviderData(
                        provider = provider,
                        isMaintenance = false,
                        isIgnored = isIgnored
                    )
                )

                if(isConfigEmpty) {
                    providersPreferences.add(
                        ProviderPreference(name = provider.name)
                    )
                }
            }

            appSettingsManager.updateData(
                appSettings.copy(providers = providersPreferences)
            )
        }
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
        
        appSettings.swapProvidersOnSettings(
            fromIndex = fromIndex,
            toIndex = toIndex
        )
    }

    /**
     *
     * Swap the provider on app settings
     *
     * */
    private suspend fun AppSettings.swapProvidersOnSettings(
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


        appSettings.toggleProviderOnSettings(index)
    }


    /**
     *
     * Toggles whether to use the provider
     * on the app's settings.
     *
     * */
    private suspend fun AppSettings.toggleProviderOnSettings(index: Int) {
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