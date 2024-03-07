package com.flixclusive.data.provider

import com.flixclusive.provider.Provider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRepository @Inject constructor() {
    val providers: HashMap<String, MutableList<Provider>> = hashMapOf()

    fun add(parentPlugin: String, provider: Provider) {
        if (providers[parentPlugin] == null) {
            providers[parentPlugin] = mutableListOf(provider)
        } else {
            providers[parentPlugin]?.add(provider)
        }
    }

    /**
     *
     * Removes all providers registered to the given plugin name.
     * */
    fun remove(parentPlugin: String) {
        providers.keys.removeIf { it.equals(parentPlugin, true) }
    }
}