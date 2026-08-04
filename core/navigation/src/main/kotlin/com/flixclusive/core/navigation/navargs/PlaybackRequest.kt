package com.flixclusive.core.navigation.navargs

import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode

/**
 * What the player should play, declared up front rather than inferred at playback time.
 *
 * Passed as the player's sole nav arg (see `PlayerScreenNavArgs` in `:feature:mobile:player`).
 * `MediaMetadata` — an interface extending [java.io.Serializable] used polymorphically as a nav
 * arg elsewhere in this app — is the precedent for a sealed [java.io.Serializable] type working
 * as a Compose Destinations nav arg.
 */
sealed interface PlaybackRequest : java.io.Serializable {
    /** Play from a provider — the normal remote flow. */
    data class FromProvider(
        val media: MediaMetadata,
        val episode: Episode?,
        val preferredStreamUrl: String? = null,
        val headers: Map<String, String>? = null,
    ) : PlaybackRequest

    /** Play a completed download. The player resolves the
     * `com.flixclusive.core.database.entity.downloads.DownloadItem`, its file, and its media
     * metadata itself from [downloadItemId]. */
    data class FromDownload(
        val downloadItemId: String
    ) : PlaybackRequest
}
