package com.flixclusive.data.backup.model

import com.flixclusive.core.database.entity.watched.WatchStatus
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

sealed interface BackupWatchProgress {
    val filmId: String
    val progress: Long
    val status: WatchStatus
    val duration: Long
    val createdAt: Long
    val updatedAt: Long
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class BackupWatchMovieProgress(
    @ProtoNumber(1) override val filmId: String,
    @ProtoNumber(2) override val progress: Long,
    @ProtoNumber(3) override val status: WatchStatus,
    @ProtoNumber(4) override val duration: Long,
    @ProtoNumber(7) override val createdAt: Long,
    @ProtoNumber(8) override val updatedAt: Long,
) : BackupWatchProgress

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class BackupWatchEpisodeProgress(
    @ProtoNumber(1) override val filmId: String,
    @ProtoNumber(2) override val progress: Long,
    @ProtoNumber(3) override val status: WatchStatus,
    @ProtoNumber(4) override val duration: Long,
    @ProtoNumber(7) override val createdAt: Long,
    @ProtoNumber(8) override val updatedAt: Long,
    @ProtoNumber(5) val seasonNumber: Int,
    @ProtoNumber(6) val episodeNumber: Int,
) : BackupWatchProgress
