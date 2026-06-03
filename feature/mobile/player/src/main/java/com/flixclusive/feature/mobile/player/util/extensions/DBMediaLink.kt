package com.flixclusive.feature.mobile.player.util.extensions

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.flixclusive.core.database.entity.provider.DBStream
import com.flixclusive.core.database.entity.provider.DBSubtitle
import com.flixclusive.core.presentation.player.model.track.PlayerServer
import com.flixclusive.core.presentation.player.model.track.PlayerSubtitle
import com.flixclusive.core.presentation.player.model.track.TrackSource

/** Filters alive+non-expired streams, deduplicates names, and maps to [PlayerServer]. */
internal fun List<DBStream>.toPlayerServers(): List<PlayerServer> {
    val names = mutableMapOf<String, Int>()
    return fastMapNotNull { stream ->
        if (!stream.isValid) return@fastMapNotNull null

        val count = names[stream.label] ?: 0
        val label = if (count > 0) "${stream.label} $count" else stream.label
        names[stream.label] = count + 1
        PlayerServer(
            label = label,
            url = stream.url,
            isDead = stream.isDead,
            headers = stream.customHeaders ?: emptyMap(),
            source = TrackSource.REMOTE,
        )
    }
}

/** Deduplicates subtitle languages and maps to [PlayerSubtitle]. */
internal fun List<DBSubtitle>.toPlayerSubtitles(): List<PlayerSubtitle> {
    val names = mutableMapOf<String, Int>()
    return fastMap { subtitle ->
        val count = names[subtitle.label] ?: 0
        val label = if (count > 0) "${subtitle.label} $count" else subtitle.label
        names[subtitle.label] = count + 1
        PlayerSubtitle(
            label = label,
            url = subtitle.url,
            isDead = subtitle.isDead,
            source = TrackSource.REMOTE,
        )
    }
}
