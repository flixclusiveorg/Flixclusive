package com.flixclusive.domain.updater

import android.content.Context
import androidx.core.app.NotificationCompat
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.util.android.notify
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.domain.updater.util.findProviderData
import com.flixclusive.model.provider.ProviderData
import com.flixclusive.provider.Provider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR


private typealias VersionCode = Long
private const val CHANNEL_UPDATER_ID = "PROVIDER_UPDATER_CHANNEL_ID"
private const val CHANNEL_UPDATER_NAME = "PROVIDER_UPDATER_CHANNEL"

@Singleton
class ProviderUpdaterUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val providerManager: ProviderManager,
    private val appSettingsManager: AppSettingsManager,
    private val client: OkHttpClient,
) {
    // Synchronized to avoid ConcurrentModificationException
    val cachedProviders: MutableMap<String, CachedData> = Collections.synchronizedMap(HashMap())
    val updatedProvidersMap: MutableMap<String, VersionCode> = Collections.synchronizedMap(HashMap())
    val outdatedProviders: MutableList<String> = Collections.synchronizedList(ArrayList())

    private var notificationChannelHasBeenInitialized = false

    suspend fun checkForUpdates(notify: Boolean) {
        val appSettingsProvider = appSettingsManager.providerSettings.data.first()

        outdatedProviders.clear()
        providerManager.providers.forEach { (name, provider) ->
            if (isProviderOutdated(provider)) outdatedProviders.add(name)
        }

        infoLog("Available updates [${outdatedProviders.size}] ${outdatedProviders.joinToString(", ")}")
        if (!notify || outdatedProviders.size == 0)
            return


        val updatableProviders = outdatedProviders.joinToString(", ")

        val notificationBody = when {
            appSettingsProvider.isUsingAutoUpdateProviderFeature -> {
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

    suspend fun isProviderOutdated(providerData: ProviderData): Boolean {
        val provider = providerManager.providers[providerData.name]
            ?: return false

        return isProviderOutdated(provider)
    }

    private suspend fun isProviderOutdated(provider: Provider): Boolean {
        val manifest = provider.manifest
        if (manifest?.updateUrl == null
            || manifest.updateUrl.equals(""))
            return false

        try {
            val updateInfo = getLatestProviderData(provider)
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

    suspend fun getLatestProviderData(providerName: String): ProviderData? {
        val provider = providerManager.providers[providerName]
        requireNotNull(provider) {
            "No such provider: $providerName"
        }

        return getLatestProviderData(provider)
    }

    private suspend fun getLatestProviderData(provider: Provider): ProviderData? {
        val manifest = provider.manifest

        if (manifest?.updateUrl == null
            || manifest.updateUrl.equals(""))
            return null

        val name = provider.name
        val cached = cachedProviders[manifest.updateUrl]

        if (cached != null && cached.time > System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)) {
            return cached.data.findProviderData(name)
        }

        val updaterJsonRequest = withIOContext {
            client.request(manifest.updateUrl!!)
                .execute()
                .body?.string()
                ?: return@withIOContext null
        } ?: return null

        val updaterJson = fromJson<List<ProviderData>>(updaterJsonRequest)
        cachedProviders[manifest.updateUrl!!] = CachedData(updaterJson)

        return updaterJson.findProviderData(name)
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
        val providerData = providerManager.providerDataList.find {
            it.name.equals(providerName, true)
        } ?: throw NoSuchElementException("No such provider data: $providerName")

        val updateInfo = getLatestProviderData(providerName)
            ?: return false

        providerManager.reloadProvider(providerData)
        updatedProvidersMap[providerName] = updateInfo.versionCode
        return true
    }

    class CachedData(var data: List<ProviderData>) {
        var time: Long = System.currentTimeMillis()
    }
}