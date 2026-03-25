package com.flixclusive.core.database.dao.films

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.film.DBFilmExternalId
import com.flixclusive.core.database.entity.film.DBFilmWithExternalIds
import kotlinx.coroutines.flow.Flow

@Dao
interface DBFilmDao {
    @Transaction
    @Query("SELECT * FROM films WHERE id = :id")
    suspend fun get(id: String): DBFilmWithExternalIds?

    @Transaction
    @Query("SELECT * FROM films")
    fun getAllAsFlow(): Flow<List<DBFilmWithExternalIds>>

    @Upsert
    suspend fun upsertFilm(media: DBFilm)

    @Upsert
    suspend fun upsertIds(list: List<DBFilmExternalId>)

    @Update
    suspend fun update(media: DBFilm)

    @Query("DELETE FROM films WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM films")
    suspend fun deleteAll()
}
