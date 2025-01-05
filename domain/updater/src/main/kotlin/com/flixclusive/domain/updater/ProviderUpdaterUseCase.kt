package com.flixclusive.domain.updater

import android.content.Context
import androidx.core.app.NotificationCompat
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.util.android.notify
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.domain.updater.util.findProviderMetadata
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.provider.Provider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR


private typealias VersionCode = Long
private const val CHANNEL_UPDATER_ID = "PROVIDER_UPDATER_CHANNEL_ID"
private const val CHANNEL_UPDATER_NAME = "PROVIDER_UPDATER_CHANNEL"

@Singleton
class ProviderUpdaterUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val providerManager: ProviderManager,
    private val dataStoreManager: DataStoreManager,
    private val client: OkHttpClient,
) {
    // Synchronized to avoid ConcurrentModificationException
    private val cachedProviders: MutableMap<String, CachedData> = Collections.synchronizedMap(HashMap())
    private val updatedProvidersMap: MutableMap<String, VersionCode> = Collections.synchronizedMap(HashMap())
    private val outdatedProviders: MutableList<String> = Collections.synchronizedList(ArrayList())

    private var notificationChannelHasBeenInitialized = false

    suspend fun checkForUpdates(notify: Boolean) {
        val providerPreferences = dataStoreManager
            .getUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY)
            .first()

        outdatedProviders.clear()
        providerManager.providers.forEach { (name, provider) ->
            if (isProviderOutdated(provider)) outdatedProviders.add(name)
        }

        infoLog("Available updates [${outdatedProviders.size}] ${outdatedProviders.joinToString(", ")}")
        if (!notify || outdatedProviders.size == 0)
            return


        val updatableProviders = outdatedProviders.joinToString(", ")

        val notificationBody = when {
            providerPreferences.autoUpdate -> {
                val res = updateAllProviders()
                if (res == 0) return

                if (res == -1) {
                    context.getString(LocaleR.string.failed_to_auto_update_providers_error_message)
                } else {
                    context.getString(LocaleR.string.providers_updated_format,
                        updatableProviders)
                }
            }
            outdatedProviders.size > 0 -> {
                context.getString(LocaleR.string.updates_out_now_provider_format, updatableProviders)
            }
            else -> context.getString(LocaleR.string.all_providers_updated)
        }

        context.notify(
            id = (System.currentTimeMillis() / 1000).toInt(),
            channelId = CHANNEL_UPDATER_ID,
            channelName = CHANNEL_UPDATER_NAME,
            shouldInitializeChannel = !notificationChannelHasBeenInitialized
        ) {
            setContentTitle(context.getString(LocaleR.string.flixclusive_providers))
            setContentText(notificationBody)
            setSmallIcon(UiCommonR.drawable.provider_logo)
            setOnlyAlertOnce(false)
            setAutoCancel(true)
            setColorized(true)
            setSilent(true)
            setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notificationBody)
            )
        }

        notificationChannelHasBeenInitialized = true
    }

    suspend fun isProviderOutdated(providerMetadata: ProviderMetadata): Boolean {
        val provider = providerManager.providers[providerMetadata.name]
            ?: return false

        return isProviderOutdated(provider)
    }

    private suspend fun isProviderOutdated(provider: Provider): Boolean {
        val manifest = provider.manifest
        if (manifest?.updateUrl == null
            || manifest.updateUrl.equals(""))
            return false

        try {
            val updateInfo = getLatestProviderMetadata(provider)
                ?: return false

            val updatedVersion = updatedProvidersMap[provider.javaClass.simpleName]
            val isOutdated = (updatedVersion != null && updatedVersion < updateInfo.versionCode) || manifest.versionCode < updateInfo.versionCode

            return isOutdated
        } catch (e: Throwable) {
            e.printStackTrace()
            errorLog("Failed to check update for: " + provider.javaClass.getSimpleName())
        }

        return false
    }

    suspend fun getLatestProviderMetadata(providerName: String): ProviderMetadata? {
        val provider = providerManager.providers[providerName]
        requireNotNull(provider) {
            "No such provider: $providerName"
        }

        return getLatestProviderMetadata(provider)
    }

    private suspend fun getLatestProviderMetadata(provider: Provider): ProviderMetadata? {
        val manifest = provider.manifest

        if (manifest?.updateUrl == null
            || manifest.updateUrl.equals(""))
            return null

        val name = provider.name
        val cached = cachedProviders[manifest.updateUrl]

        if (cached != null && cached.time > System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)) {
            return cached.data.findProviderMetadata(name)
        }

        val updaterJsonRequest = withIOContext {
            client.request(manifest.updateUrl!!)
                .execute().body.string()
        }

        val updaterJson = fromJson<List<ProviderMetadata>>(updaterJsonRequest)
        cachedProviders[manifest.updateUrl!!] = CachedData(updaterJson)

        return updaterJson.findProviderMetadata(name)
    }

    suspend fun updateAllProviders(): Int {
        var updateCount = 0
        for (provider in outdatedProviders) {
            try {
                if (updateProvider(provider) && updateCount != -1)
                    updateCount++
            } catch (t: Throwable) {
                errorLog("Error while updating provider $provider")
                updateCount = -1
            }
        }

        outdatedProviders.clear()
        checkForUpdates(false)
        return updateCount
    }

    suspend fun updateProvider(providerName: String): Boolean {
        val oldProviderMetadata = providerManager.providerMetadataList.find {
            it.name.equals(providerName, true)
        } ?: throw NoSuchElementException("No such provider data: $providerName")

        val newProviderMetadata = getLatestProviderMetadata(providerName)
            ?: return false

        providerManager.reloadProvider(
            oldProviderMetadata,
            newProviderMetadata
        )
        updatedProvidersMap[providerName] = newProviderMetadata.versionCode
        return true
    }

    class CachedData(var data: List<ProviderMetadata>) {
        var time: Long = System.currentTimeMillis()
    }
}