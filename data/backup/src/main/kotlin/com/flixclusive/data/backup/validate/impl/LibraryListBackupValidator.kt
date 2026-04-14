package com.flixclusive.data.backup.validate.impl

import com.flixclusive.core.database.dao.library.LibraryListDao
import com.flixclusive.core.database.entity.library.LibraryListType
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.model.BackupLibraryList
import com.flixclusive.data.backup.validate.BackupValidationMode
import com.flixclusive.data.backup.validate.BackupValidator
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class LibraryListBackupValidator @Inject constructor(
    private val libraryListDao: LibraryListDao,
    private val userSessionDataStore: UserSessionDataStore,
) : BackupValidator<BackupLibraryList> {
    override suspend fun invoke(
        backup: List<BackupLibraryList>,
        mode: BackupValidationMode,
    ): Result<Set<String>> {
        return runCatching {
            val ownerId = userSessionDataStore.currentUserId.filterNotNull().first()

            when (mode) {
                BackupValidationMode.CREATE -> validateCreate(ownerId = ownerId, backup = backup)
                BackupValidationMode.RESTORE -> validateRestore(ownerId = ownerId, backup = backup)
            }
        }
    }

    private suspend fun validateCreate(ownerId: String, backup: List<BackupLibraryList>): Set<String> {
        val expectedLists = libraryListDao.getAll(userId = ownerId)

        val backupWatched = backup.firstOrNull { it.listType == LibraryListType.WATCHED }
        val backupCustomByName = backup
            .asSequence()
            .filter { it.listType == LibraryListType.CUSTOM }
            .associateBy { it.name }

        val missing = linkedSetOf<String>()
        expectedLists.forEach { expected ->
            val expectedList = expected.list
            val expectedFilmIds = expected.items.asSequence().map { it.filmId }.toSet()

            val backupList = when (expectedList.listType) {
                LibraryListType.WATCHED -> backupWatched
                LibraryListType.CUSTOM -> backupCustomByName[expectedList.name]
            }

            if (backupList == null) {
                missing.add(expectedList.name)
                return@forEach
            }

            val backupFilmIds = backupList.items.asSequence().map { it.film.id }.toSet()
            if (!backupFilmIds.containsAll(expectedFilmIds)) {
                missing.add(expectedList.name)
            }
        }

        return missing
    }

    private suspend fun validateRestore(ownerId: String, backup: List<BackupLibraryList>): Set<String> {
        val expectedLists = backup
        if (expectedLists.isEmpty()) return emptySet()

        val actualLists = libraryListDao.getAll(userId = ownerId)
        val actualWatched = actualLists.firstOrNull { it.list.listType == LibraryListType.WATCHED }
        val actualCustomByName = actualLists
            .asSequence()
            .filter { it.list.listType == LibraryListType.CUSTOM }
            .associateBy { it.name }

        val missing = linkedSetOf<String>()
        expectedLists.forEach { expected ->
            val actual = when (expected.listType) {
                LibraryListType.WATCHED -> actualWatched
                LibraryListType.CUSTOM -> actualCustomByName[expected.name]
            }

            if (actual == null) {
                missing.add(expected.name)
                return@forEach
            }

            val expectedFilmIds = expected.items
                .asSequence()
                .map { it.film.id }
                .filter { it.isNotBlank() }
                .toSet()

            val actualFilmIds = actual.items
                .asSequence()
                .map { it.filmId }
                .filter { it.isNotBlank() }
                .toSet()

            if (!actualFilmIds.containsAll(expectedFilmIds)) {
                missing.add(expected.name)
            }
        }

        return missing
    }
}
