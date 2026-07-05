package com.flixclusive.data.backup.model

import com.flixclusive.data.backup.util.serializer.DateAsLongSerializer
import com.flixclusive.model.media.common.MediaType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import java.util.Date

@OptIn(ExperimentalSerializationApi::class)
@Serializable
class BackupDbMedia(
    @ProtoNumber(1) val id: String,
    @ProtoNumber(2) val title: String,
    @ProtoNumber(3) val providerId: String,
    @ProtoNumber(4) val adult: Boolean,
    @ProtoNumber(5) val mediaType: MediaType,
    @ProtoNumber(6) val overview: String?,
    @ProtoNumber(7) val posterImage: String?,
    @ProtoNumber(8) val language: String?,
    @ProtoNumber(9) val rating: Double?,
    @ProtoNumber(10) val backdropImage: String?,
    @ProtoNumber(11)
    @Serializable(with = DateAsLongSerializer::class)
    val releaseDate: Date?,
//    @ProtoNumber(12) val year: Int?,
    @ProtoNumber(13) val createdAt: Long,
    @ProtoNumber(14) val updatedAt: Long,
    @ProtoNumber(15) val externalIds: List<BackupDbMediaExternalId> = emptyList(),
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
class BackupDbMediaExternalId(
    @ProtoNumber(1) val mediaId: String,
    @ProtoNumber(2) val providerId: String,
    @ProtoNumber(3) val source: String,
    @ProtoNumber(4) val externalId: String,
    @ProtoNumber(5) val createdAt: Long,
    @ProtoNumber(6) val updatedAt: Long,
)
