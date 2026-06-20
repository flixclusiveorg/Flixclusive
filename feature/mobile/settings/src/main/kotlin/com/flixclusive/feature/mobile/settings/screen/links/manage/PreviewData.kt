package com.flixclusive.feature.mobile.settings.screen.links.manage

import com.flixclusive.core.database.entity.provider.DBStream
import java.util.Date

internal object PreviewData {
    fun getStream(
        url: String = "https://example.com/video.mkv",
        parentId: String = "provider-1",
        label: String = "Example Stream",
        description: String? = "1080p • 2.3 GB",
        isDead: Boolean = false
    ) = DBStream(
        url = url,
        parentId = parentId,
        label = label,
        description = description,
        isDead = isDead,
        createdAt = Date(),
        updatedAt = Date()
    )

    fun getStreams(count: Int = 5): List<DBStream> {
        return List(count) { i ->
            getStream(
                url = "https://example.com/video_$i.mkv",
                label = "Example Stream $i"
            )
        }
    }
}
