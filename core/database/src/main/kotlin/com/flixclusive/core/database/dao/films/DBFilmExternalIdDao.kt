package com.flixclusive.core.database.dao.films

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.flixclusive.core.database.entity.film.DBFilmExternalId

@Dao
interface DBFilmExternalIdDao {
    @Upsert
    suspend fun upsert(id: DBFilmExternalId)

    @Upsert
    suspend fun upsert(list: List<DBFilmExternalId>)

    @Query("""
        SELECT * FROM film_external_ids
        WHERE filmId = :filmId
    """)
    suspend fun getForFilm(filmId: String): List<DBFilmExternalId>

    @Query("""
        SELECT fei.id FROM film_external_ids fei
        JOIN installed_providers ip ON fei.providerId = ip.id
        WHERE fei.filmId = :filmId AND fei.source = :source
        ORDER BY ip.sortOrder ASC
        LIMIT 1
    """)
    suspend fun getBySource(filmId: String, source: String): String?

    @Query("""
        SELECT * FROM film_external_ids
        WHERE filmId = :filmId AND providerId = :providerId
    """)
    suspend fun getByProvider(filmId: String, providerId: String): List<DBFilmExternalId>

    @Query("""
        DELETE FROM film_external_ids
        WHERE filmId = :filmId AND providerId = :providerId
    """)
    suspend fun deleteByProvider(filmId: String, providerId: String)

    @Query("""
       DELETE FROM film_external_ids
       WHERE filmId = :filmId
    """)
    suspend fun deleteForFilm(filmId: String)
}
