package com.flixclusive.core.presentation.player.util.internal

import android.webkit.URLUtil
import androidx.compose.ui.util.fastMapNotNull
import androidx.core.net.toUri
import androidx.media3.common.MediaItem.SubtitleConfiguration
import com.flixclusive.core.presentation.player.util.internal.MimeTypeParser.toMimeType
import com.flixclusive.core.presentation.player.util.internal.TracksUtil.withNumberSuffix
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.model.provider.link.SubtitleSource

internal object SubtitleUtil {
    /**
     * Determines the MIME type of the subtitle based on its file extension.
     *
     * @param url The URL of the subtitle file.
     *
     * @return A string representing the MIME type of the subtitle.
     * */
    fun getSubtitleSource(url: String): SubtitleSource {
        return when {
            url.contains("file://") -> SubtitleSource.LOCAL
            URLUtil.isValidUrl(url) -> SubtitleSource.ONLINE
            else -> SubtitleSource.EMBEDDED
        }
    }


    /**
     * Converts a list of [Subtitle] objects to a list of [SubtitleConfiguration] objects.
     * */
    fun List<Subtitle>.toSubtitleConfigurations(): List<SubtitleConfiguration> {
        if (isEmpty()) {
            return emptyList()
        }

        val sortedSubtitles =
            sortedWith(
                compareBy<Subtitle> { it.language.lowercase() }
                    .thenBy {
                        it.language
                            .first()
                            .isLetterOrDigit()
                            .not()
                    },
            )

        val names = sortedSubtitles.mapTo(HashSet()) { it.language }

        return sortedSubtitles
            .fastMapNotNull { subtitle ->
                val subtitleName = subtitle.language.withNumberSuffix(set = names)
                val uri = subtitle.url.toUri()
                val mimeType = subtitle.toMimeType()

                SubtitleConfiguration
                    .Builder(uri)
                    .setId(uri.toString())
                    .setMimeType(mimeType)
                    .setLabel(subtitleName)
                    .build()
            }
    }
}
