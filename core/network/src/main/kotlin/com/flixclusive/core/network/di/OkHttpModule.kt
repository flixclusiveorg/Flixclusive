package com.flixclusive.core.network.di

import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.network.DoHPreference
import com.flixclusive.core.datastore.util.awaitFirst
import com.flixclusive.core.network.util.okhttp.UserAgentInterceptor
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.DoHProvider.doh360
import com.flixclusive.core.util.network.DoHProvider.dohAdGuard
import com.flixclusive.core.util.network.DoHProvider.dohAliDNS
import com.flixclusive.core.util.network.DoHProvider.dohCloudflare
import com.flixclusive.core.util.network.DoHProvider.dohControlD
import com.flixclusive.core.util.network.DoHProvider.dohDNSPod
import com.flixclusive.core.util.network.DoHProvider.dohGoogle
import com.flixclusive.core.util.network.DoHProvider.dohMullvad
import com.flixclusive.core.util.network.DoHProvider.dohNajalla
import com.flixclusive.core.util.network.DoHProvider.dohQuad101
import com.flixclusive.core.util.network.DoHProvider.dohQuad9
import com.flixclusive.core.util.network.DoHProvider.dohSheCan
import com.flixclusive.core.util.network.okhttp.ignoreAllSSLErrors
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import org.conscrypt.Conscrypt
import java.security.Security
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object OkHttpModule {
    @Provides
    @Singleton
    internal fun provideClient(dataStoreManager: DataStoreManager): OkHttpClient {
        try {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        } catch (throwable: Throwable) {
            errorLog(throwable.localizedMessage ?: "Unknown error trying to support TLS 1.3")
        }

        val preferences = dataStoreManager.getSystemPrefs().awaitFirst()

        return OkHttpClient
            .Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .ignoreAllSSLErrors()
            .addInterceptor(UserAgentInterceptor(preferences.userAgent))
            .apply {
                when (preferences.dns) {
                    DoHPreference.None -> Unit
                    DoHPreference.Google -> dohGoogle()
                    DoHPreference.Cloudflare -> dohCloudflare()
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
            }.build()
    }
}
