package com.flixclusive.data.provider

import androidx.compose.runtime.mutableStateMapOf
import com.flixclusive.provider.ProviderApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderApiRepository @Inject constructor() {
    private val apiMap = mutableStateMapOf<String, ProviderApi>()

    fun get(id: String) = apiMap[id]

    fun add(id: String, api: ProviderApi) {
        apiMap[id] = api
    }

    /**
     *
     * Removes all providers registered to the given provider name.
     * */
    fun remove(id: String) {
        apiMap.remove(id)
    }

    fun clear() {
        apiMap.clear()
    }
}