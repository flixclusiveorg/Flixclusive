package com.flixclusive.domain.updater

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.util.android.createChannel
import com.flixclusive.core.util.android.notificationManager
import com.flixclusive.core.util.android.notify
import com.flixclusive.core.util.common.dispatcher.AppDispatchers
import com.flixclusive.core.util.common.dispatcher.Dispatcher
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.core.util.network.request
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.domain.updater.util.findProviderData
import com.flixclusive.gradle.entities.ProviderData
import com.flixclusive.provider.Provider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR


private typealias VersionCode = Long
private const val CHANNEL_UPDATER_ID = "PROVIDER_UPDATER_CHANNEL_ID"
private const val CHANNEL_UPDATER_NAME = "PROVIDER_UPDATER_CHANNEL"

@Singleton
class ProviderUpdaterUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    private val providerManager: ProviderManager,
    private val appSettingsManager: AppSettingsManager,
    private val client: OkHttpClient,
) {
    // Synchronized to avoid ConcurrentModificationException
    val cachedProviders: MutableMap<String, CachedData> = Collections.synchronizedMap(HashMap())
    val updatedProvidersMap: MutableMap<String, VersionCode> = Collections.synchronizedMap(HashMap())
    val availableUpdatesMap: MutableList<String> = Collections.synchronizedList(ArrayList())

    private var channelHasBeenInitialized = false

    suspend fun checkForUpdates(notify: Boolean) {
        val appSettings = appSettingsManager.appSettings.data.first()
        
        availableUpdatesMap.clear()
        providerManager.providers.forEach { (name, provider) ->
            if (checkProviderUpdate(provider)) availableUpdatesMap.add(name)
        }

        infoLog("Available updates [${availableUpdatesMap.size}]: ${availableUpdatesMap.joinToString(", ")}")
        if (!notify || availableUpdatesMap.size == 0)
            return


        val updatableProviders = availableUpdatesMap.joinToString(", ")

        val notificationBody = when {
            appSettings.isUsingAutoUpdateProviderFeature -> {
                val res = updateAllProviders()
                if (res == 0) return

                if (res == -1) {
                    context.getString(UtilR.string.failed_to_auto_update_providers_error_message)
                } else {
                    context.getString(UtilR.string.providers_updated_format,
                        updatableProviders)
                }
            }
            availableUpdatesMap.size > 0 -> {
                context.getString(UtilR.string.updates_out_now_provider_format, updatableProviders)
            }
            else -> context.getString(UtilR.string.all_providers_updated)
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !channelHasBeenInitialized) {
            context.notificationManager.createChannel(
                channelId = CHANNEL_UPDATER_ID,
                channelName = CHANNEL_UPDATER_NAME
            )

            channelHasBeenInitialized = true
        }

        context.notify(
            id = (System.currentTimeMillis() / 1000).toInt(),
            channelId = CHANNEL_UPDATER_ID,
        ) {
            setContentTitle(context.getString(UtilR.string.flixclusive_providers))
            setContentText(notificationBody)
            setSmallIcon(UiCommonR.drawable.download)
            setOnlyAlertOnce(false)
            setAutoCancel(true)
            setColorized(true)
            setSilent(true)
            setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notificationBody)
            )
        }
    }

    suspend fun checkProviderUpdate(provider: Provider): Boolean {
        val manifest = provider.manifest
        if (manifest?.updateUrl == null
            || manifest.updateUrl.equals(""))
            return false

        try {
            val updateInfo = getLatestProviderData(provider)
                ?: return false

            val updatedVersion = updatedProvidersMap[provider.javaClass.simpleName]
            val isUpdated = updatedVersion != null && updatedVersion > updateInfo.versionCode

            if (isUpdated) return false

            return manifest.versionCode > updateInfo.versionCode
        } catch (e: Throwable) {
            e.printStackTrace()
            errorLog("Failed to check update for: " + provider.javaClass.getSimpleName())
        }

        return false
    }

    private suspend fun getLatestProviderData(provider: Provider): ProviderData? {
        val manifest = provider.manifest

        if (manifest?.updateUrl == null
            || manifest.updateUrl.equals(""))
            return null
        val name = provider.getName()!!

        val cached = cachedProviders[manifest.updateUrl]
        if (cached != null
            && cached.time > System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)) {
            return cached.data.findProviderData(name)
        }

        val updaterJsonRequest = withContext(ioDispatcher) {
            client.request(manifest.updateUrl!!)
                .execute()
                .body?.string()
                ?: return@withContext null
        } ?: return null

        val updaterJson = fromJson<List<ProviderData>>(updaterJsonRequest)
        cachedProviders[manifest.updateUrl!!] = CachedData(updaterJson)

        return updaterJson.findProviderData(name)
    }

    suspend fun updateAllProviders(): Int {
        var updateCount = 0
        for (provider in availableUpdatesMap) {
            try {
                if (updateProvider(provider) && updateCount != -1)
                    updateCount++
            } catch (t: Throwable) {
                errorLog("Error while updating provider $provider")
                updateCount = -1
            }
        }

        availableUpdatesMap.clear()
        checkForUpdates(false)
        return updateCount
    }

    suspend fun updateProvider(providerName: String): Boolean {
        val providerData = providerManager.providerDataList.find {
            it.name.equals(providerName, true)
        } ?: throw NoSuchElementException("No such provider data: $providerName")
        val provider = providerManager.providers[providerName]
            ?: throw NoSuchElementException("No such provider: $providerName")

        val updateInfo = getLatestProviderData(provider)
            ?: return false

        providerManager.reloadProvider(providerData)
        updatedProvidersMap[providerName] = updateInfo.versionCode
        return true
    }

    class CachedData(var data: List<ProviderData>) {
        var time: Long = System.currentTimeMillis()
    }
}