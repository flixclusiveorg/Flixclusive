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
import com.flixclusive.data.provider.util.DownloadFailed
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.provider.ProviderMetadata
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
class ProviderUpdaterUseCase
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val providerManager: ProviderManager,
        private val dataStoreManager: DataStoreManager,
        private val client: OkHttpClient,
    ) {
        // Synchronized to avoid ConcurrentModificationException
        private val cachedProviders: MutableMap<String, CachedData> = Collections.synchronizedMap(HashMap())
        private val updatedProvidersMap: MutableMap<String, VersionCode> = Collections.synchronizedMap(HashMap())
        private val outdated = ArrayDeque<String>()

        private var notificationChannelHasBeenInitialized = false

        suspend operator fun invoke(notify: Boolean) {
            val outdatedProviders =
                providerManager.metadataList.mapNotNull { (id, metadata) ->
                    if (isOutdated(id)) {
                        metadata
                    } else {
                        null
                    }
                }

            outdated.clear()
            outdated.addAll(outdatedProviders.map { it.id })

            infoLog("Available updates [${outdated.size}] ${outdated.joinToString(", ")}")
            if (!notify) {
                return
            }

            val preferences =
                dataStoreManager
                    .getUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY)
                    .first()

            val updateResults =
                when {
                    preferences.autoUpdate -> updateAll()
                    outdatedProviders.isEmpty() -> ProviderUpdateResult.None
                    else -> ProviderUpdateResult.Outdated(outdatedProviders)
                }

            notify(updateResults)
        }

        private fun notify(result: ProviderUpdateResult) {
            val notificationBody =
                when (result) {
                    is ProviderUpdateResult.Updated -> {
                        context.getString(
                            LocaleR.string.providers_updated_format,
                            result.providers.joinToString(", "),
                        )
                    }
                    is ProviderUpdateResult.Error -> {
                        if (result.success.isNotEmpty()) {
                            context.getString(
                                LocaleR.string.providers_updated_format,
                                result.success.joinToString(", "),
                            )
                        }

                        context.getString(
                            LocaleR.string.failed_to_update_providers_format,
                            result.failed.joinToString(", "),
                        )
                    }
                    is ProviderUpdateResult.Outdated -> {
                        context.getString(
                            LocaleR.string.updates_out_now_provider_format,
                            result.providers.joinToString(", "),
                        )
                    }
                    ProviderUpdateResult.None -> context.getString(LocaleR.string.all_providers_updated)
                }

            context.notify(
                id = (System.currentTimeMillis() / 1000).toInt(),
                channelId = CHANNEL_UPDATER_ID,
                channelName = CHANNEL_UPDATER_NAME,
                shouldInitializeChannel = !notificationChannelHasBeenInitialized,
            ) {
                setContentTitle(context.getString(LocaleR.string.flixclusive_providers))
                setContentText(notificationBody)
                setSmallIcon(UiCommonR.drawable.provider_logo)
                setOnlyAlertOnce(false)
                setAutoCancel(true)
                setColorized(true)
                setSilent(true)
                setStyle(
                    NotificationCompat
                        .BigTextStyle()
                        .bigText(notificationBody),
                )
            }

            notificationChannelHasBeenInitialized = true
        }

        suspend fun isOutdated(id: String): Boolean {
            val provider =
                providerManager.providers[id]
                    ?: return false

            val manifest = provider.manifest
            if (manifest.updateUrl == null || manifest.updateUrl.equals("")) {
                return false
            }

            try {
                val updateInfo =
                    getLatestMetadata(manifest.id)
                        ?: return false

                val updatedVersion = updatedProvidersMap[provider.javaClass.simpleName]
                val isOutdated =
                    (updatedVersion != null && updatedVersion < updateInfo.versionCode) || manifest.versionCode < updateInfo.versionCode

                return isOutdated
            } catch (e: Throwable) {
                errorLog(e)
                errorLog("Failed to check update for: " + provider.javaClass.getSimpleName())
            }

            return false
        }

        suspend fun getLatestMetadata(id: String): ProviderMetadata? {
            val provider = providerManager.providers[id]
            requireNotNull(provider) {
                "No such provider with ID: $id"
            }

            val manifest = provider.manifest
            if (manifest.updateUrl.isNullOrBlank()) {
                return null
            }

            val cached = cachedProviders[manifest.updateUrl]

            if (cached != null && cached.time > System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)) {
                return findMetadata(
                    id = id,
                    updaterJson = cached.data,
                )
            }

            val updaterJson =
                withIOContext {
                    client
                        .request(manifest.updateUrl!!)
                        .execute()
                        .fromJson<List<ProviderMetadata>>()
                }

            cachedProviders[manifest.updateUrl!!] = CachedData(updaterJson)

            return findMetadata(
                id = id,
                updaterJson = updaterJson,
            )
        }

        suspend fun updateAll(): ProviderUpdateResult {
            val failed = ArrayList<ProviderMetadata>()
            val updated = ArrayList<ProviderMetadata>()

            while (outdated.isNotEmpty()) {
                val providerId = outdated.removeFirst()
                val metadata = providerManager.metadataList[providerId] ?: continue

                try {
                    update(providerId)
                } catch (t: Throwable) {
                    errorLog("Error while updating provider with ID: $providerId")
                    errorLog(t)
                    failed.add(metadata)
                }
            }

            if (failed.isNotEmpty()) {
                return ProviderUpdateResult.Error(
                    success = updated,
                    failed = failed,
                )
            }

            return ProviderUpdateResult.Updated(updated)
        }

        @Throws(DownloadFailed::class)
        suspend fun update(id: String) {
            val oldMetadata =
                providerManager.metadataList[id]
                    ?: throw NoSuchElementException("No such provider ID: $id")

            val newMetadata =
                getLatestMetadata(id)
                    ?: throw NoSuchElementException("Can't find latest metadata for provider with ID: $id")

            providerManager.update(
                oldMetadata = oldMetadata,
                newMetadata = newMetadata,
            )

            updatedProvidersMap[id] = newMetadata.versionCode
        }

        private class CachedData(
            var data: List<ProviderMetadata>,
        ) {
            var time: Long = System.currentTimeMillis()
        }

        private fun findMetadata(
            updaterJson: List<ProviderMetadata>,
            id: String,
        ): ProviderMetadata =
            updaterJson.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("Provider ID not found: $id")
    }

sealed class ProviderUpdateResult {
    data object None : ProviderUpdateResult()

    data class Updated(
        val providers: List<ProviderMetadata>,
    ) : ProviderUpdateResult()

    data class Outdated(
        val providers: List<ProviderMetadata>,
    ) : ProviderUpdateResult()

    data class Error(
        val success: List<ProviderMetadata>,
        val failed: List<ProviderMetadata>,
    ) : ProviderUpdateResult()
}
