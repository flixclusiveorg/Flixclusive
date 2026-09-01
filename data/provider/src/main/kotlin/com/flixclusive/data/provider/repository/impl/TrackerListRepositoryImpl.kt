package com.flixclusive.data.provider.repository.impl

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.provider.R
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.provider.repository.TrackerListItems
import com.flixclusive.data.provider.repository.TrackerListKey
import com.flixclusive.data.provider.repository.TrackerListRepository
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.provider.capability.TrackerFeature
import com.flixclusive.provider.capability.TrackerProviderApi
import com.flixclusive.provider.tracker.TrackerList
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

internal class TrackerListRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val providerRepository: ProviderRepository,
    private val userSessionDataStore: UserSessionDataStore,
    private val appDispatchers: AppDispatchers,
) : TrackerListRepository {
    private companion object {
        operator fun <K, V> MutableStateFlow<Map<K, V>>.set(key: K, value: V) {
            update { it + (key to value) }
        }

        operator fun <K, V> MutableStateFlow<Map<K, V>>.get(key: K): V? = value[key]

        fun <K, V> MutableStateFlow<Map<K, V>>.clear() {
            update { emptyMap() }
        }

        fun <K, V> MutableStateFlow<Map<K, V>>.remove(key: K) {
            update { it - key }
        }
    }

    private val listsMap = MutableStateFlow<Map<String, Async<List<TrackerList>>>>(emptyMap())
    private val itemsMap = MutableStateFlow<Map<TrackerListKey, Async<TrackerListItems>>>(emptyMap())
    private val membershipMap = MutableStateFlow<Map<String, Map<TrackerListKey, Boolean>>>(emptyMap())
    private val authMap = MutableStateFlow<Map<String, Async<Boolean>>>(emptyMap())

    private val locks = ConcurrentHashMap<String, Mutex>()

    private fun lockOf(key: String): Mutex = locks.getOrPut(key) { Mutex() }

    override fun getLists(providerId: String): Flow<Async<List<TrackerList>>> =
        listsMap.map { it[providerId] ?: Async.Loading }.distinctUntilChanged()

    override fun getItems(key: TrackerListKey): Flow<Async<TrackerListItems>> =
        itemsMap.map { it[key] ?: Async.Loading }.distinctUntilChanged()

    override fun getMembership(mediaId: String): Flow<Map<TrackerListKey, Boolean>> =
        membershipMap.map { it[mediaId] ?: emptyMap() }.distinctUntilChanged()

    override fun isAuthenticated(providerId: String): Flow<Async<Boolean>> =
        authMap.map { it[providerId] ?: Async.Loading }.distinctUntilChanged()

    override suspend fun loadLists(providerId: String, refresh: Boolean) {
        withContext(appDispatchers.io) {
            lockOf("lists:$providerId").withLock {
                if (!refresh && listsMap[providerId] is Async.Success) return@withLock
                if (listsMap[providerId] !is Async.Success) {
                    listsMap[providerId] = Async.Loading
                }

                listsMap[providerId] = try {
                    Async.Success(requireApi(providerId).getLists())
                } catch (e: Throwable) {
                    errorLog("Failed to load tracker lists for provider $providerId")
                    errorLog(e)
                    Async.Failure(e)
                }
            }
        }
    }

    override suspend fun loadNextItems(list: TrackerList, pageSize: Int, refresh: Boolean) {
        val key = TrackerListKey(providerId = list.providerId, listId = list.id)

        withContext(appDispatchers.io) {
            lockOf("items:${key.providerId}:${key.listId}").withLock {
                val cached = (itemsMap[key] as? Async.Success)?.data.takeUnless { refresh }
                if (cached != null && !cached.hasNextPage) return@withLock
                if (cached == null) itemsMap[key] = Async.Loading

                val nextPage = (cached?.page ?: 0) + 1
                itemsMap[key] = try {
                    val paginated = requireApi(list.providerId).getListItems(list, nextPage, pageSize)
                    Async.Success(
                        TrackerListItems(
                            items = cached?.items.orEmpty() + paginated.results,
                            page = nextPage,
                            hasNextPage = paginated.hasNextPage,
                        ),
                    )
                } catch (e: Throwable) {
                    errorLog("Failed to load items of tracker list ${list.id}")
                    errorLog(e)
                    Async.Failure(e)
                }
            }
        }
    }

    override suspend fun loadMembership(
        mediaId: String,
        list: TrackerList,
        media: MediaMetadata,
        refresh: Boolean,
    ) {
        val key = TrackerListKey(providerId = list.providerId, listId = list.id)

        withContext(appDispatchers.io) {
            lockOf("membership:$mediaId:${key.providerId}:${key.listId}").withLock {
                if (!refresh && membershipMap[mediaId]?.containsKey(key) == true) return@withLock

                val contains = try {
                    requireApi(list.providerId).isInList(list, media)
                } catch (e: Throwable) {
                    errorLog("Failed to check membership of $mediaId on tracker list ${list.id}")
                    errorLog(e)
                    return@withLock
                }

                setMembership(mediaId, key, contains)
            }
        }
    }

    override suspend fun loadAuthentication(providerId: String, refresh: Boolean) {
        withContext(appDispatchers.io) {
            lockOf("auth:$providerId").withLock {
                if (!refresh && authMap[providerId] is Async.Success) return@withLock

                authMap[providerId] = try {
                    val api = resolveApi(providerId)
                    Async.Success(api != null && api.isAuthenticated())
                } catch (e: Throwable) {
                    errorLog("Failed to resolve authentication of provider $providerId")
                    errorLog(e)
                    Async.Failure(e)
                }
            }
        }
    }

    override suspend fun createList(
        providerId: String,
        name: String,
        description: String?,
    ): Result<TrackerList> {
        return withContext(appDispatchers.io) {
            runCatching {
                val created = requireApi(providerId).createList(name, description)
                val cached = (listsMap[providerId] as? Async.Success)?.data
                if (cached != null) {
                    listsMap[providerId] = Async.Success(cached + created)
                }

                created
            }
        }
    }

    override suspend fun updateList(list: TrackerList): Result<TrackerList> {
        return withContext(appDispatchers.io) {
            runCatching {
                val updated = requireApi(list.providerId).updateList(list)
                replaceList(updated)
                updated
            }
        }
    }

    override suspend fun deleteList(list: TrackerList): Result<Unit> {
        return withContext(appDispatchers.io) {
            runCatching {
                requireApi(list.providerId).deleteList(list)

                val cached = (listsMap[list.providerId] as? Async.Success)?.data
                if (cached != null) {
                    listsMap[list.providerId] = Async.Success(cached.filterNot { it.id == list.id })
                }

                val key = TrackerListKey(providerId = list.providerId, listId = list.id)
                itemsMap.remove(key)
                membershipMap.update { current ->
                    current.mapValues { (_, lists) -> lists - key }
                }
            }
        }
    }

    override suspend fun addItem(
        mediaId: String,
        list: TrackerList,
        media: MediaMetadata,
    ): Result<TrackerList> = toggleItem(mediaId, list, media, isAdding = true)

    override suspend fun removeItem(
        mediaId: String,
        list: TrackerList,
        media: MediaMetadata,
    ): Result<TrackerList> = toggleItem(mediaId, list, media, isAdding = false)

    private suspend fun toggleItem(
        mediaId: String,
        list: TrackerList,
        media: MediaMetadata,
        isAdding: Boolean,
    ): Result<TrackerList> {
        return withContext(appDispatchers.io) {
            runCatching {
                require(media.providerId == list.providerId) {
                    context.getString(R.string.tracker_not_matched_provider)
                }

                val api = requireApi(list.providerId)
                if (isAdding) {
                    api.addListItem(list, media)
                } else {
                    api.removeListItem(list, media)
                }

                val key = TrackerListKey(providerId = list.providerId, listId = list.id)
                setMembership(mediaId, key, isAdding)
                itemsMap.remove(key)

                val updated = runCatching { api.getList(list.id) }.getOrDefault(list)
                replaceList(updated)
                updated
            }
        }
    }

    override suspend fun getApi(providerId: String, feature: TrackerFeature): TrackerProviderApi? {
        return withContext(appDispatchers.io) {
            try {
                val api = resolveApi(providerId) ?: return@withContext null
                if (!api.getFeatures().contains(feature)) return@withContext null
                if (!api.isAuthenticated()) return@withContext null
                api
            } catch (e: Throwable) {
                errorLog("Failed to resolve tracker API of provider $providerId")
                errorLog(e)
                null
            }
        }
    }

    override fun invalidate(providerId: String?) {
        if (providerId == null) {
            listsMap.clear()
            itemsMap.clear()
            membershipMap.clear()
            authMap.clear()
            return
        }

        listsMap.remove(providerId)
        authMap.remove(providerId)
        itemsMap.update { current -> current.filterKeys { it.providerId != providerId } }
        membershipMap.update { current ->
            current.mapValues { (_, lists) -> lists.filterKeys { it.providerId != providerId } }
        }
    }

    private fun setMembership(mediaId: String, key: TrackerListKey, contains: Boolean) {
        membershipMap.update { current ->
            current + (mediaId to (current[mediaId].orEmpty() + (key to contains)))
        }
    }

    private fun replaceList(list: TrackerList) {
        val cached = (listsMap[list.providerId] as? Async.Success)?.data ?: return
        val index = cached.indexOfFirst { it.id == list.id }
        if (index < 0) return

        listsMap[list.providerId] = Async.Success(
            cached.toMutableList().apply { set(index, list) },
        )
    }

    private suspend fun resolveApi(providerId: String): TrackerProviderApi? {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        val provider = providerRepository.getProvider(providerId, userId) ?: return null
        if (!provider.isTrackerEnabled) return null

        return provider.plugin?.getTrackerApi(context)
    }

    private suspend fun requireApi(providerId: String): TrackerProviderApi {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        val provider = providerRepository.getProvider(providerId, userId)
            ?: error(context.getString(R.string.tracker_api_unavailable))

        val name = provider.name ?: providerId
        val api = provider.plugin?.getTrackerApi(context)
            ?: error(context.getString(R.string.tracker_api_unavailable))

        if (!api.getFeatures().contains(TrackerFeature.LIST_MANAGEMENT)) {
            error(context.getString(R.string.tracker_no_list_management_feature, name))
        }

        if (!api.isAuthenticated()) {
            error(context.getString(R.string.tracker_requires_authentication, name))
        }

        return api
    }
}
