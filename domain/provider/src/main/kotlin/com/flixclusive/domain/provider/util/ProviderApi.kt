package com.flixclusive.domain.provider.util

import com.flixclusive.core.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.TvShow
import com.flixclusive.provider.ProviderApi
import com.flixclusive.core.locale.R as LocaleR

/**
 *
 * Method to be called if a [Film] directly
 * comes from TMDB Meta Provider. This method
 * obtains the watch id of the film based on the
 * given [ProviderApi].
 *
 * @param film The detailed film object of the film.
 * @param api The provider api to be used to obtain the watch id.
 * */
internal suspend fun ProviderApi.getWatchId(film: FilmMetadata?): Resource<String?> {
    return withIOContext {
        try {
            if (film == null) {
                return@withIOContext Resource.Failure(LocaleR.string.default_error)
            }

            var i = 1
            var watchId: String? = null
            val maxPage = 3

            while (watchId == null) {
                if (i > maxPage) {
                    return@withIOContext Resource.Success(null)
                }

                val searchResponse =
                    search(
                        title = film.title,
                        id = film.identifier,
                        imdbId = film.imdbId,
                        tmdbId = film.tmdbId,
                        page = i,
                    )

                if (searchResponse.results.isEmpty()) {
                    return@withIOContext Resource.Success(null)
                }

                for (item in searchResponse.results) {
                    val respectiveId = item.getRespectiveId(film)
                    if (respectiveId != null) {
                        watchId = respectiveId
                        break
                    }

                    val filmTypeMatches = item.filmType.type == film.filmType.type
                    val titleMatches = item.title.titleMatches(film.title)
                    val releaseDateMatches = item.dateMatches(film)
                    val yearReleaseMatches = item.yearMatches(film)

                    if (titleMatches && filmTypeMatches && film is TvShow) {
                        if (yearReleaseMatches || releaseDateMatches) {
                            watchId = item.identifier
                            break
                        }

                        val tvShowInfo = getMetadata(film = item)

                        if (
                            tvShowInfo.yearMatches(film) ||
                            film.seasonCountMatches(tvShowInfo)
                        ) {
                            watchId = item.identifier
                            break
                        }
                    }

                    if (titleMatches && filmTypeMatches && (yearReleaseMatches || releaseDateMatches)) {
                        watchId = item.identifier
                        break
                    }
                }

                if (searchResponse.hasNextPage) {
                    i++
                } else if (watchId.isNullOrEmpty()) {
                    break
                }
            }

            Resource.Success(watchId)
        } catch (e: Throwable) {
            errorLog(e)
            Resource.Failure(
                UiText.StringResource(
                    LocaleR.string.failed_to_fetch_media_id_message,
                    e.actualMessage,
                ),
            )
        }
    }
}

private fun String.compareIgnoringSymbols(other: String?): Boolean {
    val lettersAndNumbersOnlyRegex = Regex("[^A-Za-z0-9 ]")

    val normalizedThis = this.replace(lettersAndNumbersOnlyRegex, "")
    val normalizedOther = other?.replace(lettersAndNumbersOnlyRegex, "")

    return normalizedThis.equals(normalizedOther, ignoreCase = true)
}

private fun Film.yearMatches(other: Film) =
    when {
        other.year == null || this.year == null -> false
        else -> this.year == other.year
    }

private fun Film.dateMatches(other: Film) =
    when {
        other.releaseDate.isNullOrBlank() || this.releaseDate.isNullOrBlank() -> false
        else -> releaseDate.equals(other.releaseDate, true)
    }

private fun String.titleMatches(other: String?) =
    when {
        other.isNullOrBlank() || isNullOrBlank() -> false
        else -> equals(other, true) || compareIgnoringSymbols(other)
    }

private fun TvShow.seasonCountMatches(other: FilmMetadata) = this.seasons.size == (other as TvShow).totalSeasons

private fun Film.getRespectiveId(other: Film): String? {
    return when {
        id == other.id -> identifier
        tmdbId == other.tmdbId -> identifier
        imdbId == other.imdbId -> identifier
        identifier == other.identifier -> identifier
        else -> null
    }
}
