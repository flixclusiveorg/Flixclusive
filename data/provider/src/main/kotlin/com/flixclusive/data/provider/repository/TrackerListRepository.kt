package com.flixclusive.data.provider.repository

import com.flixclusive.core.common.domain.Async
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.provider.capability.TrackerFeature
import com.flixclusive.provider.capability.TrackerProviderApi
import com.flixclusive.provider.tracker.TrackerList
import kotlinx.coroutines.flow.Flow

data class TrackerListKey(
    val providerId: String,
    val listId: String,
)

data class TrackerListItems(
    val items: List<MediaMetadata> = emptyList(),
    val page: Int = 0,
    val hasNextPage: Boolean = true,
)

interface TrackerListRepository {
    fun getLists(providerId: String): Flow<Async<List<TrackerList>>>

    fun getItems(key: TrackerListKey): Flow<Async<TrackerListItems>>

    fun getMembership(mediaId: String): Flow<Map<TrackerListKey, Boolean>>

    fun isAuthenticated(providerId: String): Flow<Async<Boolean>>

    suspend fun loadLists(providerId: String, refresh: Boolean = false)

    suspend fun loadNextItems(list: TrackerList, pageSize: Int = DEFAULT_PAGE_SIZE, refresh: Boolean = false)

    suspend fun loadMembership(
        mediaId: String,
        list: TrackerList,
        media: MediaMetadata,
        refresh: Boolean = false,
    )

    suspend fun loadAuthentication(providerId: String, refresh: Boolean = false)

    suspend fun createList(providerId: String, name: String, description: String? = null): Result<TrackerList>

    suspend fun updateList(list: TrackerList): Result<TrackerList>

    suspend fun deleteList(list: TrackerList): Result<Unit>

    suspend fun addItem(mediaId: String, list: TrackerList, media: MediaMetadata): Result<TrackerList>

    suspend fun removeItem(mediaId: String, list: TrackerList, media: MediaMetadata): Result<TrackerList>

    suspend fun getApi(providerId: String, feature: TrackerFeature): TrackerProviderApi?

    fun invalidate(providerId: String? = null)

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }
}
