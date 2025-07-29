package com.flixclusive.domain.provider

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.log.LogRule
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.provider.Repository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GetRepositoryUseCaseTest {
    @get:Rule
    val rule = LogRule()

    private lateinit var useCaseClass: GetRepositoryUseCase

    @Before
    fun setUp() {
        val dataFlow = MutableStateFlow<Preferences>(mutablePreferencesOf())
        dataFlow.value = mutablePreferencesOf(UserPreferences.PROVIDER_PREFS_KEY to "{}")

        useCaseClass =
            GetRepositoryUseCase(
                client = OkHttpClient(),
                dataStoreManager =
                    mockk {
                        every { userPreferences.data } returns dataFlow
                        every { getUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY) } returns
                            dataFlow
                                .map {
                                    Json.decodeFromString(
                                        it[UserPreferences.PROVIDER_PREFS_KEY].toString(),
                                    )
                                }
                    },
            )
    }

    @Test
    fun `test normal GitHub URLs`() =
        runTest {
            val url = "https://github.com/flixclusiveorg/provider-template"
            val response = useCaseClass.invoke(url)

            assert(response is Resource.Success)
            println(response.data?.toFormattedString())
        }

    @Test
    fun `test normal raw GitHub URLs`() =
        runTest {
            val url = "https://raw.githubusercontent.com/flixclusiveorg/provider-template/builds/updater.json"
            val response = useCaseClass.invoke(url)

            assert(response is Resource.Success)
            println(response.data?.toFormattedString())
        }

    private fun Repository.toFormattedString(): String {
        return """
            URL: $url
            Owner: $owner
            Name: $name
            RawURL: $rawLinkFormat
            """.trimIndent()
    }
}
