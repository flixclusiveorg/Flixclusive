package com.flixclusive.data.backup.restore.impl

import com.flixclusive.core.database.dao.library.LibraryListDao
import com.flixclusive.core.database.dao.library.LibraryListItemDao
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListType
import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.media.DBMediaExternalId
import com.flixclusive.core.database.entity.media.DBMediaFts
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.model.BackupDbMedia
import com.flixclusive.data.backup.model.BackupDbMediaExternalId
import com.flixclusive.data.backup.model.BackupLibraryList
import com.flixclusive.data.backup.restore.BackupRestorer
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject

internal class LibraryListBackupRestorer @Inject constructor(
    private val libraryListDao: LibraryListDao,
    private val libraryListItemDao: LibraryListItemDao,
    private val userSessionDataStore: UserSessionDataStore,
) : BackupRestorer<BackupLibraryList> {
    override suspend fun invoke(items: List<BackupLibraryList>): Result<Unit> {
        return runCatching {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()

            // Restore all media metadata (including WATCHED list items)
            items.forEach { list ->
                list.items.forEach { item ->
                    val media = item.media.toDbMedia()
                    val externalIds = item.media.externalIds.map { it.toDbMediaExternalId() }

                    libraryListItemDao.upsertMedia(media)
                    libraryListItemDao.upsertMediaFts(item.media.toDbMediaFts())
                    libraryListItemDao.upsertIds(externalIds)
                }
            }

            // Restore only custom lists to avoid duplicating WATCHED lists.
            items
                .filter { it.listType == LibraryListType.CUSTOM }
                .forEach { list ->
                    val newList = LibraryList(
                        ownerId = userId,
                        name = list.name,
                        description = list.description,
                        listType = list.listType,
                        createdAt = Date(list.createdAt),
                        updatedAt = Date(list.updatedAt),
                    )

                    libraryListDao.insert(newList)

                    list.items.forEach { item ->
                        libraryListItemDao.insertItem(
                            LibraryListItem(
                                mediaId = item.media.id,
                                listId = newList.id,
                                createdAt = Date(item.createdAt),
                                updatedAt = Date(item.updatedAt),
                            )
                        )
                    }
                }
        }
    }

    private fun BackupDbMedia.toDbMedia(): DBMedia {
        return DBMedia(
            id = id,
            title = title,
            providerId = providerId,
            adult = adult,
            type = mediaType,
            overview = overview,
            posterImage = posterImage,
            language = language,
            rating = rating,
            backdropImage = backdropImage,
            releaseDate = releaseDate,
            createdAt = Date(createdAt),
            updatedAt = Date(updatedAt),
        )
    }

    private fun BackupDbMedia.toDbMediaFts(): DBMediaFts {
        return DBMediaFts(
            mediaId = id,
            title = title,
            overview = overview ?: "",
        )
    }

    private fun BackupDbMediaExternalId.toDbMediaExternalId(): DBMediaExternalId {
        return DBMediaExternalId(
            mediaId = mediaId,
            providerId = providerId,
            source = source,
            externalId = externalId,
            createdAt = Date(createdAt),
            updatedAt = Date(updatedAt),
        )
    }
}
