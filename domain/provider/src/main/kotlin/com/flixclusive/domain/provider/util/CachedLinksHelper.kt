package com.flixclusive.domain.provider.util

import com.flixclusive.data.provider.cache.CacheKey
import com.flixclusive.data.provider.cache.CachedLinks
import com.flixclusive.data.provider.cache.CachedLinksRepository
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.provider.link.Stream

internal class CachedLinksHelper(
    private val cachedLinksRepository: CachedLinksRepository,
) {
    fun getCacheForFilm(
        film: FilmMetadata,
        episode: Episode?,
        preferredProvider: String?,
        hasProviders: Boolean,
    ): CachedLinks? {
        return when {
            preferredProvider != null -> getCacheForProvider(
                film = film,
                episode = episode,
                providerId = preferredProvider
            )
            !hasProviders || film.isFromTmdb -> getCacheForTmdb(film = film, episode = episode)
            else -> null
        }
    }

    fun isCacheValid(
        cache: CachedLinks?,
        preferredProvider: String?,
    ): Boolean {
        return cache?.isCached(preferredProvider) == true
    }

    fun storeTmdbCache(
        film: FilmMetadata,
        episode: Episode?,
        streams: List<Stream>,
    ) {
        val tmdbKey = CacheKey.create(
            filmId = film.identifier,
            providerId = DEFAULT_FILM_SOURCE_NAME,
            episode = episode,
        )

        val cache = CachedLinks(
            watchId = film.identifier,
            providerId = DEFAULT_FILM_SOURCE_NAME,
            thumbnail = film.backdropImage ?: film.posterImage,
            streams = streams,
        )

        cachedLinksRepository.storeCache(tmdbKey, cache)
    }

    fun refreshProviderCache(
        film: FilmMetadata,
        episode: Episode?,
        providerId: String,
    ): Boolean {
        val cacheKey = CacheKey.create(
            filmId = film.identifier,
            providerId = providerId,
            episode = episode,
        )

        val existingCache = cachedLinksRepository.getCache(cacheKey)
        return if (existingCache != null) {
            cachedLinksRepository.removeCache(cacheKey)
            cachedLinksRepository.storeCache(cacheKey, existingCache)
            true
        } else {
            false
        }
    }

    private fun getCacheForProvider(
        film: FilmMetadata,
        episode: Episode?,
        providerId: String,
    ): CachedLinks? {
        val cacheKey = CacheKey.create(
            filmId = film.identifier,
            providerId = providerId,
            episode = episode,
        )
        return cachedLinksRepository.getCache(cacheKey)
    }

    private fun getCacheForTmdb(
        film: FilmMetadata,
        episode: Episode?,
    ): CachedLinks? {
        val cacheKey = CacheKey.create(
            filmId = film.identifier,
            providerId = DEFAULT_FILM_SOURCE_NAME,
            episode = episode,
        )
        return cachedLinksRepository.getCache(cacheKey)
    }
}
