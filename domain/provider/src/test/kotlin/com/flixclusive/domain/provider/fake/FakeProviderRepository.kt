package com.flixclusive.domain.provider.fake

import android.content.Context
import com.flixclusive.data.provider.ProviderRepository
import com.flixclusive.data.provider.util.CollectionsOperation
import com.flixclusive.model.datastore.user.ProviderFromPreferences
import com.flixclusive.model.provider.Author
import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.ProviderType
import com.flixclusive.model.provider.Status
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import dalvik.system.PathClassLoader
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient

class FakeProviderRepository : ProviderRepository {
    private val providers = mutableMapOf<String, Provider>()
    private val metadata = mutableMapOf<String, ProviderMetadata>()
    private val preferences = mutableListOf<ProviderFromPreferences>()
    private val operations = MutableSharedFlow<CollectionsOperation.List<ProviderFromPreferences>>()

    override suspend fun add(
        provider: Provider,
        classLoader: PathClassLoader,
        metadata: ProviderMetadata,
        preferenceItem: ProviderFromPreferences,
    ) {
        providers[metadata.id] = provider
        this.metadata[metadata.id] = metadata
        addToPreferences(preferenceItem)
    }

    override suspend fun addToPreferences(preferenceItem: ProviderFromPreferences) {
        if (!preferences.contains(preferenceItem)) {
            preferences.add(preferenceItem)
            operations.emit(CollectionsOperation.List.Add(preferenceItem))
        }
    }

    override fun getProviderMetadata(id: String): ProviderMetadata? = metadata[id]

    override fun getProvider(id: String): Provider? = providers[id]

    override fun getProviderFromPreferences(id: String): ProviderFromPreferences? = preferences.find { it.id == id }

    override fun getEnabledProviders(): List<ProviderMetadata> =
        preferences
            .filter { !it.isDisabled }
            .mapNotNull { metadata[it.id] }

    override fun getProviders(): List<ProviderMetadata> = metadata.values.toList()

    override fun getOrderedProviders(): List<ProviderMetadata> = preferences.mapNotNull { metadata[it.id] }

    override fun observe(): SharedFlow<CollectionsOperation.List<ProviderFromPreferences>> = operations.asSharedFlow()

    override suspend fun moveProvider(
        fromIndex: Int,
        toIndex: Int,
    ) {
        if (fromIndex in preferences.indices && toIndex in preferences.indices) {
            val item = preferences.removeAt(fromIndex)
            preferences.add(toIndex, item)
        }
    }

    override suspend fun remove(id: String) {
        providers.remove(id)
        metadata.remove(id)
        removeFromPreferences(id)
    }

    override suspend fun clearAll() {
        providers.clear()
        metadata.clear()
        preferences.clear()
    }

    override suspend fun removeFromPreferences(id: String) {
        val item = preferences.find { it.id == id }
        if (item != null && preferences.remove(item)) {
            operations.emit(CollectionsOperation.List.Remove(item))
        }
    }

    override suspend fun toggleProvider(id: String) {
        val index = preferences.indexOfFirst { it.id == id }
        if (index != -1) {
            val provider = preferences[index]
            preferences[index] = provider.copy(isDisabled = !provider.isDisabled)
        }
    }

    suspend fun addMockProvider(
        id: String,
        name: String = "Mock Provider $id",
        isEnabled: Boolean = true,
    ) {
        val mockMetadata = ProviderMetadata(
            id = id,
            name = name,
            versionName = "1.0.0",
            authors = listOf(Author("Test Author")),
            repositoryUrl = "https://example.com",
            buildUrl = "https://example.com/build",
            changelog = "",
            versionCode = 1,
            language = Language("en"),
            providerType = ProviderType.All,
            iconUrl = "https://example.com/icon.png",
            description = "Mock provider for testing",
            status = Status.Beta,
        )

        val mockPreference = ProviderFromPreferences(
            id = id,
            isDisabled = !isEnabled,
            name = name,
            filePath = "mock/path/to/provider/$id",
        )

        metadata[id] = mockMetadata
        if (!preferences.any { it.id == id }) {
            preferences.add(mockPreference)
        }

        val provider = object : Provider() {
            override fun getApi(context: Context, client: OkHttpClient): ProviderApi {
                return object : ProviderApi() { }
            }
        }

        add(
            provider = provider,
            classLoader = PathClassLoader("", ClassLoader.getSystemClassLoader()),
            metadata = mockMetadata,
            preferenceItem = mockPreference,
        )
    }
}
