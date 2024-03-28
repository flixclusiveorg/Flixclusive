package com.flixclusive.data.provider

import com.flixclusive.provider.ProviderApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRepository @Inject constructor() {
    val providers: HashMap<String, MutableList<ProviderApi>> = hashMapOf()

    fun add(providerName: String, providerApi: ProviderApi) {
        if (providers[providerName] == null) {
            providers[providerName] = mutableListOf(providerApi)
        } else {
            providers[providerName]?.add(providerApi)
        }
    }

    /**
     *
     * Removes all providers registered to the given provider name.
     * */
    fun remove(providerName: String) {
        providers.keys.removeIf { it.equals(providerName, true) }
    }
}