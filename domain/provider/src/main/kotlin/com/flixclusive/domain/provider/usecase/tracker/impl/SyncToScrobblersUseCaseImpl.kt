package com.flixclusive.domain.provider.usecase.tracker.impl

import android.content.Context
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.util.retryWithBackoff
import com.flixclusive.core.database.entity.watched.WatchProgress
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.data.provider.ProviderCapability
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.provider.repository.TrackerListRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.tracker.SyncToScrobblersUseCase
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.provider.capability.TrackerFeature
import com.flixclusive.provider.capability.TrackerProviderApi
import com.flixclusive.provider.tracker.ScrobbleAction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class SyncToScrobblersUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userSessionDataStore: UserSessionDataStore,
    private val providerRepository: ProviderRepository,
    private val trackerListRepository: TrackerListRepository,
) : SyncToScrobblersUseCase {
    override fun invoke(
        action: ScrobbleAction,
        watchProgress: WatchProgress,
        media: MediaMetadata,
        episode: Episode?,
    ): Flow<Async<Unit>> = channelFlow {
        val scrobblers = getScrobblers()

        if (scrobblers.isEmpty()) {
            warnLog("No authenticated scrobble providers found, skipping scrobble sync")
            return@channelFlow
        }

        val percentage = (watchProgress.progress.toFloat() / watchProgress.duration.toFloat()) * 100f

        scrobblers.mapAsync { (provider, api) ->
            try {
                retryWithBackoff(
                    maxDelayMs = 30_000L
                ) {
                    api.scrobble(
                        action = action,
                        media = media,
                        progressPercent = percentage,
                        episode = episode,
                        atMs = watchProgress.progress
                    )
                }
            } catch (e: Throwable) {
                errorLog("Failed to scrobble progress to provider $provider: ${e.message}")
                e.printStackTrace()
                send(
                    Async.Failure(
                        message = context.getString(
                            R.string.failed_scrobbling_progress_to_provider,
                            provider,
                            e.message
                        )
                    )
                )
            }
        }

        send(Async.Success(Unit))
    }

    private suspend fun getScrobblers(): List<Pair<String?, TrackerProviderApi>> {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        val providers = providerRepository.getProvidersWithCapability(userId, ProviderCapability.TRACKER)

        return providers.mapNotNull {
            val api = trackerListRepository.getApi(it.id, TrackerFeature.SCROBBLE)
            if (api == null) {
                warnLog("Provider ${it.name} cannot scrobble")
                return@mapNotNull null
            }

            it.name to api
        }
    }
}
