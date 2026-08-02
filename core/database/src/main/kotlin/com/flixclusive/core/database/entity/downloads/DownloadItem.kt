package com.flixclusive.core.database.entity.downloads

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flixclusive.model.media.common.MediaType
import java.util.Date

@Entity(
    tableName = "download_items",
    indices = [
        Index(value = ["mediaId"]),
        Index(value = ["state"]),
    ],
)
data class DownloadItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: String,
    val mediaTitle: String,
    val mediaType: MediaType,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
    val state: DownloadItemState = DownloadItemState.QUEUED,
    val phase: DownloadPhase? = null,
    val streamUrl: String? = null,
    val streamHeaders: Map<String, String>? = null,
    val streamFallbackCandidates: List<DownloadStreamCandidate>? = null,
    val streamBytesDownloaded: Long = 0,
    val streamTotalBytes: Long = 0,
    val subtitleUrl: String? = null,
    val subtitleHeaders: Map<String, String>? = null,
    val subtitleBytesDownloaded: Long = 0,
    val subtitleTotalBytes: Long = 0,
    val subtitleError: String? = null,
    val errorMessage: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
)
