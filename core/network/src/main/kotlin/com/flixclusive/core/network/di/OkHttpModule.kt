package com.flixclusive.core.network.di

import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.OkHttpHelper.ignoreAllSSLErrors
import com.flixclusive.core.util.network.doh360
import com.flixclusive.core.util.network.dohAdGuard
import com.flixclusive.core.util.network.dohAliDNS
import com.flixclusive.core.util.network.dohCloudflare
import com.flixclusive.core.util.network.dohControlD
import com.flixclusive.core.util.network.dohDNSPod
import com.flixclusive.core.util.network.dohGoogle
import com.flixclusive.core.util.network.dohMullvad
import com.flixclusive.core.util.network.dohNajalla
import com.flixclusive.core.util.network.dohQuad101
import com.flixclusive.core.util.network.dohQuad9
import com.flixclusive.core.util.network.dohSheCan
import com.flixclusive.model.datastore.network.DoHPreference
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.conscrypt.Conscrypt
import java.security.Security
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OkHttpModule {

    @Provides
    @Singleton
    internal fun provideClient(
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
                    DoHPreference.None -> Unit
                    DoHPreference.Google -> dohGoogle()
                    DoHPreference.Cloudfare -> dohCloudflare()
                    DoHPreference.AdGuard -> dohAdGuard()
                    DoHPreference.Quad9 -> dohQuad9()
                    DoHPreference.AliDNS -> dohAliDNS()
                    DoHPreference.DNSPod -> dohDNSPod()
                    DoHPreference.DNS360 -> doh360()
                    DoHPreference.Quad101 -> dohQuad101()
                    DoHPreference.Mullvad -> dohMullvad()
                    DoHPreference.ControlD -> dohControlD()
                    DoHPreference.Najalla -> dohNajalla()
                    DoHPreference.SheCan -> dohSheCan()
                }
            }
            .build()
    }
}