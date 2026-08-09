package com.flixclusive.data.provider.util.extensions

import com.flixclusive.core.database.entity.provider.CachedStream
import com.flixclusive.core.database.entity.provider.CachedSubtitle
import com.flixclusive.model.provider.link.Flag
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.model.provider.link.SubtitleSource

/** The reverse of [toCachedLink] — used when a cached link needs to be handed back to something
 * (like the download queue) that expects the original provider-facing models. */
fun CachedStream.toStream(): Stream {
    val flags = buildSet {
        if (isThirdPartyGateway) {
            add(Flag.ThirdPartyGateway(name = thirdPartyGatewayName ?: label, logo = thirdPartyGatewayLogo))
        }
        expiresOn?.let { add(Flag.Expires(it)) }
        customHeaders?.let { add(Flag.RequiresAuth(it)) }
    }

    return Stream(
        name = label,
        url = url,
        description = description,
        flags = flags.takeIf { it.isNotEmpty() },
    )
}

fun CachedSubtitle.toSubtitle(): Subtitle {
    val flags = buildSet {
        customHeaders?.let { add(Flag.RequiresAuth(it)) }
    }

    return Subtitle(
        language = label,
        type = SubtitleSource.entries.firstOrNull { it.name == subtitleSource } ?: SubtitleSource.ONLINE,
        url = url,
        flags = flags.takeIf { it.isNotEmpty() },
    )
}
