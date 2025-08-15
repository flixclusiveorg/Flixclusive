package com.flixclusive.data.provider.repository.impl.fake

import android.content.Context
import com.flixclusive.model.provider.ProviderManifest
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import okhttp3.OkHttpClient

class TestProvider : Provider() {
    companion object {
        const val TEST_PROVIDER_ID = "test-provider"
    }

    init {
        manifest = ProviderManifest(
            id = TEST_PROVIDER_ID,
            name = "Test Provider",
            versionName = "",
            versionCode = 1,
            updateUrl = "",
            providerClassName = "",
            requiresResources = false
        )
    }

    override fun getApi(context: Context, client: OkHttpClient): ProviderApi {
        return TestProviderApi()
    }
}
