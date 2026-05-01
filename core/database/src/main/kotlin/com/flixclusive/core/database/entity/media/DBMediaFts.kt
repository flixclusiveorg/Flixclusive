package com.flixclusive.core.database.entity.media

import androidx.room.Entity
import androidx.room.Fts3
import com.flixclusive.model.media.MediaMetadata

@Fts3
@Entity(tableName = "media_fts")
data class DBMediaFts(
    val mediaId: String,
    val title: String,
    val overview: String,
) {
    companion object {
        fun MediaMetadata.toDBMediaFts() = DBMediaFts(
            mediaId = id,
            overview = overview ?: "",
            title = title,
        )
    }
}
