package com.flixclusive.domain.provider.updater

import android.content.Context
import androidx.core.app.NotificationCompat
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.ProviderFromPreferences
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.datastore.util.awaitFirst
import com.flixclusive.core.util.android.notify
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.coroutines.blockFirstNotNull
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.manage.PROVIDER_DEBUG
import com.flixclusive.domain.provider.manage.ProviderLoaderUseCase
import com.flixclusive.domain.provider.manage.ProviderUnloaderUseCase
import com.flixclusive.domain.provider.util.extensions.DownloadFailed
import com.flixclusive.domain.provider.util.extensions.createFileForProvider
import com.flixclusive.domain.provider.util.extensions.downloadProvider
import com.flixclusive.model.provider.ProviderMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import java.io.File
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.set
import com.flixclusive.core.strings.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

private typealias VersionCode = Long

private const val CHANNEL_UPDATER_ID = "PROVIDER_UPDATER_CHANNEL_ID"
private const val CHANNEL_UPDATER_NAME = "PROVIDER_UPDATER_CHANNEL"

@Singleton
class ProviderUpdaterUseCase
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val dataStoreManager: DataStoreManager,
        private val userSessionDataStore: UserSessionDataStore,
        private val providerRepository: ProviderRepository,
        private val providerLoaderUseCase: ProviderLoaderUseCase,
        private val providerUnloaderUseCase: ProviderUnloaderUseCase,
        private val client: OkHttpClient,
    ) {
        // Synchronized to avoid ConcurrentModificationException
        private val cachedProviders: MutableMap<String, CachedData> =
            Collections.synchronizedMap(HashMap())
        private val updatedProvidersMap: MutableMap<String, VersionCode> =
            Collections.synchronizedMap(HashMap())
        private val outdated = ArrayDeque<String>()

        private var notificationChannelHasBeenInitialized = false

        private val providerPreferences: ProviderPreferences
            get() =
            dataStoreManager
                .getUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY)
                .awaitFirst()

        suspend operator fun invoke(notify: Boolean) {
            val outdatedProviders = getOutdatedProviders()
            outdated.clear()
            outdated.addAll(outdatedProviders.map { it.id })

            infoLog("Available updates [${outdated.size}] ${outdated.joinToString(", ")}")
            if (!notify) return

            val preferences =
                dataStoreManager
                    .getUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY)
                    .first()

            val updateResults =
                when {
                    preferences.isAutoUpdateEnabled -> updateAll()
                    outdatedProviders.isEmpty() -> ProviderUpdateResult.None
                    else -> ProviderUpdateResult.Outdated(outdatedProviders)
                }

            notify(updateResults)
        }

        private suspend fun getOutdatedProviders(): List<ProviderMetadata> =
            providerRepository.getOrderedProviders().mapNotNull { metadata ->
                if (isOutdated(metadata.id)) metadata else null
            }

        private fun notify(result: ProviderUpdateResult) {
            val notificationBody =
                when (result) {
                    is ProviderUpdateResult.Updated -> {
                        context.getString(
                            LocaleR.string.providers_updated_format,
                            result.providers.joinToString(", ") { it.name },
                        )
                    }

                    is ProviderUpdateResult.Error -> {
                        if (result.success.isNotEmpty()) {
                            context.getString(
                                LocaleR.string.providers_updated_format,
                                result.success.joinToString(", ") { it.name },
                            )
                        }

                        context.getString(
                            LocaleR.string.failed_to_update_providers_format,
                            result.failed.joinToString(", ") { it.name },
                        )
                    }

                    is ProviderUpdateResult.Outdated -> {
                        context.getString(
                            LocaleR.string.updates_out_now_provider_format,
                            result.providers.joinToString(", ") { it.name },
                        )
                    }

                    ProviderUpdateResult.None -> {
                        return
                    }
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
            if (id.endsWith(PROVIDER_DEBUG)) return false

            val provider =
                providerRepository.getProvider(id)
                    ?: return false

            val manifest = provider.manifest
            if (manifest.updateUrl == null || manifest.updateUrl.equals("")) {
                return false
            }

            try {
                val updateInfo =
                    getLatestMetadata(manifest.id)
                        ?: return false

                val updatedVersion = updatedProvidersMap[manifest.id]
                val isOutdated =
                    (updatedVersion != null && updatedVersion < updateInfo.versionCode) ||
                        manifest.versionCode < updateInfo.versionCode

                return isOutdated
            } catch (e: Throwable) {
                errorLog(e)
                errorLog("Failed to check update for: " + provider.javaClass.getSimpleName())
            }

            return false
        }

        suspend fun getLatestMetadata(id: String): ProviderMetadata? {
            val provider = providerRepository.getProvider(id)
            requireNotNull(provider) {
                "No such provider with ID: $id"
            }

            val manifest = provider.manifest
            if (manifest.updateUrl.isNullOrBlank()) {
                return null
            }

            val cached = cachedProviders[manifest.updateUrl]

            if (cached != null &&
                cached.time > System.currentTimeMillis() -
                TimeUnit.MINUTES.toMillis(
                    30,
                )
            ) {
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
            if (outdated.isEmpty()) return ProviderUpdateResult.None

            val failed = ArrayList<ProviderMetadata>()
            val updated = ArrayList<ProviderMetadata>()

            while (outdated.isNotEmpty()) {
                val providerId = outdated.removeFirst()
                val metadata = providerRepository.getProviderMetadata(providerId) ?: continue

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
                providerRepository.getProviderMetadata(id)
                    ?: throw NoSuchElementException("No such provider ID: $id")

            val newMetadata =
                getLatestMetadata(id)
                    ?: throw NoSuchElementException("Can't find latest metadata for provider with ID: $id")

            update(
                oldMetadata = oldMetadata,
                newMetadata = newMetadata,
            )

            updatedProvidersMap[id] = newMetadata.versionCode
        }

        @Throws(DownloadFailed::class)
        suspend fun update(
            oldMetadata: ProviderMetadata,
            newMetadata: ProviderMetadata,
        ) {
            requireNotNull(providerRepository.getProvider(oldMetadata.id)) {
                "No such provider: ${oldMetadata.name}"
            }

            val newPreference =
                getUpdatedPreferenceItem(
                    id = oldMetadata.id,
                    newMetadata = newMetadata,
                )

            providerRepository.addToPreferences(preferenceItem = newPreference)

            client.downloadProvider(
                saveTo = File(newPreference.filePath),
                buildUrl = newMetadata.buildUrl,
            )

            providerUnloaderUseCase.unload(
                metadata = oldMetadata,
                unloadOnPreferences = false,
            )

            providerLoaderUseCase.load(
                provider = newMetadata,
                filePath = newPreference.filePath,
            )
        }

        private fun getUpdatedPreferenceItem(
            id: String,
            newMetadata: ProviderMetadata,
        ): ProviderFromPreferences {
            val oldOrderPosition =
                providerPreferences
                    .providers
                    .indexOfFirst { it.id == id }

            val oldPreference = providerPreferences.providers[oldOrderPosition]

            val userId = userSessionDataStore.currentUserId.blockFirstNotNull()!!
            val file =
                context
                    .createFileForProvider(
                        userId = userId,
                        provider = newMetadata,
                    )
            val filePath = file.absolutePath

            return oldPreference.copy(
                name = newMetadata.name,
                filePath = filePath,
            )
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
