package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flixclusive.core.database.entity.film.DBFilm

@Dao
interface DBFilmDao {
    @Query("SELECT * FROM films WHERE id = :id")
    suspend fun get(id: String): DBFilm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: DBFilm)

    @Update
    suspend fun update(media: DBFilm)

    @Query("DELETE FROM films WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM films")
    suspend fun deleteAll()
}
