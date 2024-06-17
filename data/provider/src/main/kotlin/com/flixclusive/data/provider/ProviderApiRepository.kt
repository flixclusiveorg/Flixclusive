package com.flixclusive.data.provider

import com.flixclusive.provider.ProviderApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderApiRepository @Inject constructor() {
    val apiMap: HashMap<String, ProviderApi> = hashMapOf()

    fun add(providerName: String, providerApi: ProviderApi) {
        if (apiMap[providerName] == null) {
            apiMap[providerName] = providerApi
        } else {
            apiMap[providerName] = providerApi
        }
    }

    /**
     *
     * Removes all providers registered to the given provider name.
     * */
    fun remove(providerName: String) {
        apiMap.keys.removeIf { it.equals(providerName, true) }
    }
}