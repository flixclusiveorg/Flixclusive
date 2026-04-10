package com.flixclusive.data.backup.restore.impl

import com.flixclusive.core.database.dao.library.LibraryListDao
import com.flixclusive.core.database.dao.library.LibraryListItemDao
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.film.DBFilmExternalId
import com.flixclusive.core.database.entity.film.DBFilmFts
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListType
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.model.BackupDbFilm
import com.flixclusive.data.backup.model.BackupDbFilmExternalId
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

            libraryListDao.deleteAllExceptWatched(ownerId = userId)

            // Restore all film metadata (including WATCHED list items)
            items.forEach { list ->
                list.items.forEach { item ->
                    val film = item.film.toDbFilm()
                    val externalIds = item.film.externalIds.map { it.toDbFilmExternalId() }

                    libraryListItemDao.upsertFilm(film)
                    libraryListItemDao.upsertFilmFts(item.film.toDbFilmFts())
                    libraryListItemDao.upsertIds(externalIds)
                }
            }

            // Restore only custom lists to avoid duplicating WATCHED lists.
            items
                .filter { it.listType == LibraryListType.CUSTOM }
                .forEach { list ->
                    val newListId = libraryListDao.insert(
                        LibraryList(
                            ownerId = userId,
                            name = list.name,
                            description = list.description,
                            listType = list.listType,
                            createdAt = Date(list.createdAt),
                            updatedAt = Date(list.updatedAt),
                        )
                    ).toInt()

                    list.items.forEach { item ->
                        libraryListItemDao.insertItem(
                            LibraryListItem(
                                filmId = item.film.id,
                                listId = newListId,
                                createdAt = Date(item.createdAt),
                                updatedAt = Date(item.updatedAt),
                            )
                        )
                    }
                }
        }
    }

    private fun BackupDbFilm.toDbFilm(): DBFilm {
        return DBFilm(
            id = id,
            title = title,
            providerId = providerId,
            adult = adult,
            filmType = filmType,
            overview = overview,
            posterImage = posterImage,
            language = language,
            rating = rating,
            backdropImage = backdropImage,
            releaseDate = releaseDate,
            year = year,
            createdAt = Date(createdAt),
            updatedAt = Date(updatedAt),
        )
    }

    private fun BackupDbFilm.toDbFilmFts(): DBFilmFts {
        return DBFilmFts(
            filmId = id,
            title = title,
            overview = overview ?: "",
        )
    }

    private fun BackupDbFilmExternalId.toDbFilmExternalId(): DBFilmExternalId {
        return DBFilmExternalId(
            filmId = filmId,
            providerId = providerId,
            source = source,
            externalId = externalId,
            createdAt = Date(createdAt),
            updatedAt = Date(updatedAt),
        )
    }
}
