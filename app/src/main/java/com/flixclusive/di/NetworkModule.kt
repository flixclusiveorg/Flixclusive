package com.flixclusive.di

import android.app.Application
import com.flixclusive.common.Constants.GITHUB_BASE_URL
import com.flixclusive.common.Constants.TMDB_API_BASE_URL
import com.flixclusive.utils.LoggerUtils.errorLog
import com.flixclusive.data.api.GithubConfigService
import com.flixclusive.data.api.TMDBApiService
import com.flixclusive.domain.common.PaginatedSearchItemsDeserializer
import com.flixclusive.domain.common.SearchItemDeserializer
import com.flixclusive.domain.model.tmdb.TMDBPageResponse
import com.flixclusive.domain.model.tmdb.TMDBSearchItem
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.domain.preferences.AppSettingsManager
import com.flixclusive.providers.utils.network.OkHttpUtils.ignoreAllSSLErrors
import com.flixclusive.providers.utils.network.doh360
import com.flixclusive.providers.utils.network.dohAdGuard
import com.flixclusive.providers.utils.network.dohAliDNS
import com.flixclusive.providers.utils.network.dohCloudflare
import com.flixclusive.providers.utils.network.dohControlD
import com.flixclusive.providers.utils.network.dohDNSPod
import com.flixclusive.providers.utils.network.dohGoogle
import com.flixclusive.providers.utils.network.dohMullvad
import com.flixclusive.providers.utils.network.dohNajalla
import com.flixclusive.providers.utils.network.dohQuad101
import com.flixclusive.providers.utils.network.dohQuad9
import com.flixclusive.providers.utils.network.dohSheCan
import com.flixclusive.service.network.NetworkConnectivityObserver
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.conscrypt.Conscrypt
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.Security
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        appSettingsManager: AppSettingsManager,
    ): OkHttpClient {
        try {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        } catch (throwable: Throwable) {
            errorLog(throwable.localizedMessage ?: "Unknown error trying to support TLS 1.3")
        }

        val dns = runBlocking { appSettingsManager.appSettings.data.map { it.dns }.first() }

        return OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .ignoreAllSSLErrors()
            .apply {
                when (dns) {
                    AppSettings.Companion.DoHPreference.None -> Unit
                    AppSettings.Companion.DoHPreference.Google -> dohGoogle()
                    AppSettings.Companion.DoHPreference.Cloudfare -> dohCloudflare()
                    AppSettings.Companion.DoHPreference.AdGuard -> dohAdGuard()
                    AppSettings.Companion.DoHPreference.Quad9 -> dohQuad9()
                    AppSettings.Companion.DoHPreference.AliDNS -> dohAliDNS()
                    AppSettings.Companion.DoHPreference.DNSPod -> dohDNSPod()
                    AppSettings.Companion.DoHPreference.DNS360 -> doh360()
                    AppSettings.Companion.DoHPreference.Quad101 -> dohQuad101()
                    AppSettings.Companion.DoHPreference.Mullvad -> dohMullvad()
                    AppSettings.Companion.DoHPreference.ControlD -> dohControlD()
                    AppSettings.Companion.DoHPreference.Najalla -> dohNajalla()
                    AppSettings.Companion.DoHPreference.SheCan -> dohSheCan()
                }
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideTMDBApiService(
        client: OkHttpClient
    ): TMDBApiService {
        val tmdbPageResponse = TMDBPageResponse<TMDBSearchItem>()

        val gson = GsonBuilder()
            .registerTypeAdapter(TMDBSearchItem::class.java, SearchItemDeserializer())
            .registerTypeAdapter(tmdbPageResponse::class.java, PaginatedSearchItemsDeserializer())
            .create()

        return Retrofit.Builder()
            .baseUrl(TMDB_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()
            .create(TMDBApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideNetworkConnectivityObserver(
        application: Application,
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
    ) = NetworkConnectivityObserver(application, mainDispatcher)

    @Provides
    @Singleton
    fun provideGithubConfigService(
        client: OkHttpClient
    ): GithubConfigService =
        Retrofit.Builder()
            .baseUrl(GITHUB_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(GithubConfigService::class.java)
}