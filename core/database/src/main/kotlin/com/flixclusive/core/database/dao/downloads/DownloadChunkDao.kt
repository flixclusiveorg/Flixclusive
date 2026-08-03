package com.flixclusive.core.database.dao.downloads

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flixclusive.core.database.entity.downloads.DownloadChunk
import com.flixclusive.core.database.entity.downloads.DownloadChunkStatus

@Dao
interface DownloadChunkDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(chunks: List<DownloadChunk>): List<Long>

    @Query("SELECT * FROM download_chunks WHERE downloadItemId = :downloadItemId ORDER BY chunkIndex ASC")
    suspend fun getChunksForItem(downloadItemId: String): List<DownloadChunk>

    @Query("UPDATE download_chunks SET bytesDownloaded = :bytesDownloaded, status = :status WHERE id = :id")
    suspend fun updateProgress(
        id: Long,
        bytesDownloaded: Long,
        status: DownloadChunkStatus,
    )

    @Query("DELETE FROM download_chunks WHERE downloadItemId = :downloadItemId")
    suspend fun deleteChunksForItem(downloadItemId: String)
}
