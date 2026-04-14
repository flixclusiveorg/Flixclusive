package com.flixclusive.data.backup.model

import com.flixclusive.model.film.util.FilmType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
class BackupDbFilm(
    @ProtoNumber(1) val id: String,
    @ProtoNumber(2) val title: String,
    @ProtoNumber(3) val providerId: String,
    @ProtoNumber(4) val adult: Boolean,
    @ProtoNumber(5) val filmType: FilmType,
    @ProtoNumber(6) val overview: String?,
    @ProtoNumber(7) val posterImage: String?,
    @ProtoNumber(8) val language: String?,
    @ProtoNumber(9) val rating: Double?,
    @ProtoNumber(10) val backdropImage: String?,
    @ProtoNumber(11) val releaseDate: String?,
    @ProtoNumber(12) val year: Int?,
    @ProtoNumber(13) val createdAt: Long,
    @ProtoNumber(14) val updatedAt: Long,
    @ProtoNumber(15) val externalIds: List<BackupDbFilmExternalId> = emptyList(),
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
class BackupDbFilmExternalId(
    @ProtoNumber(1) val filmId: String,
    @ProtoNumber(2) val providerId: String,
    @ProtoNumber(3) val source: String,
    @ProtoNumber(4) val externalId: String,
    @ProtoNumber(5) val createdAt: Long,
    @ProtoNumber(6) val updatedAt: Long,
)
