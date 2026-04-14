package com.flixclusive.data.backup.create.impl

import com.flixclusive.core.database.dao.library.LibraryListDao
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.film.DBFilmExternalId
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.create.BackupCreator
import com.flixclusive.data.backup.model.BackupDbFilm
import com.flixclusive.data.backup.model.BackupDbFilmExternalId
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
        val film = metadata.toBackupItem(externalIds)
        return BackupLibraryListItem(
            listId = item.listId,
            film = film,
            createdAt = item.createdAt.time,
            updatedAt = item.updatedAt.time,
        )
    }

    private fun DBFilmExternalId.toBackupItem(): BackupDbFilmExternalId {
        return BackupDbFilmExternalId(
            filmId = filmId,
            providerId = providerId,
            source = source,
            externalId = externalId,
            createdAt = createdAt.time,
            updatedAt = updatedAt.time,
        )
    }

    private fun DBFilm.toBackupItem(externalIds: List<BackupDbFilmExternalId>): BackupDbFilm {
        return BackupDbFilm(
            id = id,
            title = title,
            overview = overview,
            releaseDate = releaseDate,
            providerId = providerId,
            adult = adult,
            filmType = filmType,
            posterImage = posterImage,
            language = language,
            rating = rating,
            backdropImage = backdropImage,
            year = year,
            externalIds = externalIds,
            createdAt = createdAt.time,
            updatedAt = updatedAt.time,
        )
    }
}
