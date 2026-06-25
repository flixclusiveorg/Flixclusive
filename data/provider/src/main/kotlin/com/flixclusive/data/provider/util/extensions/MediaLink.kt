package com.flixclusive.data.provider.util.extensions

import com.flixclusive.core.database.entity.provider.CachedMediaLink
import com.flixclusive.core.database.entity.provider.CachedStream
import com.flixclusive.core.database.entity.provider.CachedSubtitle
import com.flixclusive.model.provider.link.Flag
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle

fun MediaLink.toCachedLink(
    providerId: String,
    ownerId: String,
    mediaId: String,
    episodeNumber: Int?,
    seasonNumber: Int?,
): CachedMediaLink {
    return when (this) {
        is Stream -> toCachedStream(
            providerId = providerId,
            ownerId = ownerId,
            mediaId = mediaId,
            episodeNumber = episodeNumber,
            seasonNumber = seasonNumber,
        )
        is Subtitle -> toCachedSubtitle(
            providerId = providerId,
            ownerId = ownerId,
            mediaId = mediaId,
            episodeNumber = episodeNumber,
            seasonNumber = seasonNumber,
        )
    }
}

private fun Stream.toCachedStream(
    providerId: String,
    ownerId: String,
    mediaId: String,
    episodeNumber: Int?,
    seasonNumber: Int?,
): CachedStream {
    val expiresFlag = flags?.filterIsInstance<Flag.Expires>()?.firstOrNull()
    val requiresAuthFlag = flags?.filterIsInstance<Flag.RequiresAuth>()?.firstOrNull()
    val thirdPartyFlag = flags?.filterIsInstance<Flag.ThirdPartyGateway>()?.firstOrNull()

    return CachedStream(
        url = url,
        label = name,
        description = description,
        expiresOn = expiresFlag?.expiresOn,
        customHeaders = requiresAuthFlag?.customHeaders?.takeIf { it.isNotEmpty() },
        isThirdPartyGateway = thirdPartyFlag != null,
        thirdPartyGatewayName = thirdPartyFlag?.name,
        thirdPartyGatewayLogo = thirdPartyFlag?.logo,
        providerId = providerId,
        ownerId = ownerId,
        mediaId = mediaId,
        episodeNumber = episodeNumber,
        seasonNumber = seasonNumber,
    )
}

private fun Subtitle.toCachedSubtitle(
    providerId: String,
    ownerId: String,
    mediaId: String,
    episodeNumber: Int?,
    seasonNumber: Int?,
): CachedSubtitle {
    val requiresAuthFlag = flags?.filterIsInstance<Flag.RequiresAuth>()?.firstOrNull()

    return CachedSubtitle(
        url = url,
        label = language,
        subtitleSource = type.name,
        customHeaders = requiresAuthFlag?.customHeaders?.takeIf { it.isNotEmpty() },
        providerId = providerId,
        ownerId = ownerId,
        mediaId = mediaId,
        episodeNumber = episodeNumber,
        seasonNumber = seasonNumber,
    )
}
