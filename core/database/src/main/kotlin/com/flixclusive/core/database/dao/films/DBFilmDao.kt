package com.flixclusive.core.database.dao.films

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flixclusive.core.database.entity.film.DBFilm
import kotlinx.coroutines.flow.Flow

@Dao
interface DBFilmDao {
    @Query("SELECT * FROM films WHERE id = :id")
    suspend fun get(id: String): DBFilm?

    @Query("SELECT * FROM films")
    fun getAllAsFlow(): Flow<List<DBFilm>>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(media: DBFilm)

    @Update
    suspend fun update(media: DBFilm)

    @Query("DELETE FROM films WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM films")
    suspend fun deleteAll()
}
