package com.flixclusive.core.presentation.player.model

import androidx.compose.runtime.Stable
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import com.flixclusive.core.presentation.player.model.PlaylistItem.Companion.create
import com.flixclusive.core.presentation.player.util.internal.MimeTypeParser
import com.flixclusive.core.presentation.player.util.internal.SubtitleUtil.toSubtitleConfigurations
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import java.util.Locale

/**
 * A data class representing an item in the playlist.
 *
 * To create an instance of [PlaylistItem], use the [create] factory method.
 * */
@Stable
data class PlaylistItem internal constructor(
    val id: String,
    val providerId: String,
    val title: String,
    val streams: List<Stream>,
    val subtitles: List<Subtitle>,
) {
    companion object {
        /**
         * Factory method to create a [PlaylistItem] from [FilmMetadata], [Stream], and a list of [Subtitle].
         * Optionally, an [Episode] can be provided for TV shows.
         *
         * @param metadata The film metadata.
         * @param stream The stream information.
         * @param subtitles The list of subtitles.
         * @param episode The episode information, if applicable.
         *
         * @return A new instance of [PlaylistItem].
         * */
        fun create(
            metadata: FilmMetadata,
            stream: List<Stream>,
            subtitles: List<Subtitle>,
            episode: Episode? = null,
        ) = PlaylistItem(
            id = createId(metadata, episode),
            title = formatTitle(metadata, episode),
            providerId = metadata.providerId,
            streams = stream,
            subtitles = subtitles,
        )

        /**
         * Generates a unique identifier for the playlist item based on the film metadata and episode information.
         *
         * @param metadata The film metadata.
         * @param episode The episode information, if applicable.
         *
         * @return A unique identifier string.
         * */
        internal fun createId(
            metadata: FilmMetadata,
            episode: Episode? = null
        ): String {
            return if (metadata.filmType == FilmType.MOVIE) {
                metadata.identifier
            } else {
                "${metadata.id}_S${episode!!.season}_E${episode.number}"
            }
        }

        private const val FILM_TV_SHOW_TITLE_FORMAT = "S%d E%d: %s"

        /**
         * Formats the title based on the film type and episode information.
         *
         * @param film The film metadata.
         * @param episode The episode information, if applicable.
         *
         * @return The formatted title.
         * */
        private fun formatTitle(
            film: Film,
            episode: Episode? = null
        ): String {
            if (episode == null && film.filmType == FilmType.TV_SHOW) {
                return film.title
            }

            return when (film.filmType) {
                FilmType.MOVIE -> film.title
                FilmType.TV_SHOW -> String.format(
                    Locale.ROOT,
                    FILM_TV_SHOW_TITLE_FORMAT,
                    episode!!.season,
                    episode.number,
                    episode.title
                )
            }
        }

        /**
         * Converts the [PlaylistItem] to a [MediaItem] for use with ExoPlayer.
         *
         * @param preferredStream The index of the preferred stream quality.
         * */
        internal fun PlaylistItem.toMediaItem(preferredStream: Int): MediaItem {
            val metadata = MediaMetadata.Builder().setDisplayTitle(title).build()
            val subtitleConfigurations = subtitles.toSubtitleConfigurations()
            val url = streams[preferredStream].url

            return MediaItem
                .Builder()
                .setUri(url)
                .setMediaId(url)
                .setSubtitleConfigurations(subtitleConfigurations)
                .setMediaMetadata(metadata)
                .apply {
                    if (MimeTypeParser.isM3U8(url)) {
                        setMimeType(MimeTypes.APPLICATION_M3U8)
                    }
                }.build()
        }
    }
}
