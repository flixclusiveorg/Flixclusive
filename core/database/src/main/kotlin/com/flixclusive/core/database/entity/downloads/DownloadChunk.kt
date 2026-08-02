package com.flixclusive.core.database.entity.downloads

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "download_chunks",
    foreignKeys = [
        ForeignKey(
            entity = DownloadItem::class,
            parentColumns = ["id"],
            childColumns = ["downloadItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["downloadItemId"]),
    ],
)
data class DownloadChunk(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val downloadItemId: Long,
    val chunkIndex: Int,
    val rangeStart: Long,
    val rangeEnd: Long,
    val bytesDownloaded: Long = 0,
    val status: DownloadChunkStatus = DownloadChunkStatus.PENDING,
)
