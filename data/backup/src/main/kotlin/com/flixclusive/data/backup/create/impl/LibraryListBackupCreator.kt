package com.flixclusive.data.backup.create.impl

import com.flixclusive.core.database.dao.library.LibraryListDao
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.media.DBMediaExternalId
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.create.BackupCreator
import com.flixclusive.data.backup.model.BackupDbMedia
import com.flixclusive.data.backup.model.BackupDbMediaExternalId
import com.flixclusive.data.backup.model.BackupLibraryList
import com.flixclusive.data.backup.model.BackupLibraryListItem
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class LibraryListBackupCreator @Inject constructor(
    private val libraryListDao: LibraryListDao,
    private val userSessionDataStore: UserSessionDataStore,
) : BackupCreator<BackupLibraryList> {
    override suspend fun invoke(): Result<List<BackupLibraryList>> {
        return runCatching {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val lists = libraryListDao.getAll(userId = userId)

            lists.map { list ->
                BackupLibraryList(
                    name = list.name,
                    description = list.description,
                    listType = list.list.listType,
                    items = list.items.map { item ->
                        item.toBackupItem()
                    },
                    createdAt = list.list.createdAt.time,
                    updatedAt = list.list.updatedAt.time,
                )
            }
        }
    }

    private fun LibraryListItemWithMetadata.toBackupItem(): BackupLibraryListItem {
        val externalIds = externalIds.map { it.toBackupItem() }
        val media = metadata.toBackupItem(externalIds)
        return BackupLibraryListItem(
            listId = item.listId,
            media = media,
            createdAt = item.createdAt.time,
            updatedAt = item.updatedAt.time,
        )
    }

    private fun DBMediaExternalId.toBackupItem(): BackupDbMediaExternalId {
        return BackupDbMediaExternalId(
            mediaId = mediaId,
            providerId = providerId,
            source = source,
            externalId = externalId,
            createdAt = createdAt.time,
            updatedAt = updatedAt.time,
        )
    }

    private fun DBMedia.toBackupItem(externalIds: List<BackupDbMediaExternalId>): BackupDbMedia {
        return BackupDbMedia(
            id = id,
            title = title,
            overview = overview,
            releaseDate = releaseDate,
            providerId = providerId,
            adult = adult,
            mediaType = type,
            posterImage = posterImage,
            language = language,
            rating = rating,
            backdropImage = backdropImage,
            externalIds = externalIds,
            createdAt = createdAt.time,
            updatedAt = updatedAt.time,
        )
    }
}
