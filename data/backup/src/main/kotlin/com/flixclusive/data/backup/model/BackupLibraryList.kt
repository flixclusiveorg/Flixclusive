package com.flixclusive.data.backup.model

import com.flixclusive.core.database.entity.library.LibraryListType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BackupLibraryList(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val description: String?,
    @ProtoNumber(3) val listType: LibraryListType,
    @ProtoNumber(4) val items: List<BackupLibraryListItem>,
    @ProtoNumber(5) val createdAt: Long,
    @ProtoNumber(6) val updatedAt: Long,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BackupLibraryListItem(
    @ProtoNumber(1) val listId: Int,
    @ProtoNumber(2) val film: BackupDbFilm,
    @ProtoNumber(3) val createdAt: Long,
    @ProtoNumber(4) val updatedAt: Long,
)
