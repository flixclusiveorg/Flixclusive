package com.flixclusive.data.provider

import androidx.compose.runtime.mutableStateMapOf
import com.flixclusive.provider.ProviderApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderApiRepository @Inject constructor() {
    val apiMap = mutableStateMapOf<String, ProviderApi>()

    fun add(providerName: String, providerApi: ProviderApi) {
        apiMap[providerName] = providerApi
    }

    /**
     *
     * Removes all providers registered to the given provider name.
     * */
    fun remove(providerName: String) {
        apiMap.remove(providerName)
    }
}