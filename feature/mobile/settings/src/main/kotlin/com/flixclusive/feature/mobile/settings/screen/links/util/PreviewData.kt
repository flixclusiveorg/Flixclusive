package com.flixclusive.feature.mobile.settings.screen.links.util

import com.flixclusive.core.database.entity.provider.CachedStream
import java.util.Date

internal object PreviewData {
    fun getStream(
        url: String = "https://example.com/video.mkv",
        providerId: String = "provider-1",
        ownerId: String = "owner-1",
        mediaId: String = "media-1",
        label: String = "Example Stream",
        description: String? = "1080p • 2.3 GB",
        isDead: Boolean = false
    ) = CachedStream(
        url = url,
        providerId = providerId,
        ownerId = ownerId,
        mediaId = mediaId,
        label = label,
        description = description,
        isDead = isDead,
        createdAt = Date(),
        updatedAt = Date()
    )
}
