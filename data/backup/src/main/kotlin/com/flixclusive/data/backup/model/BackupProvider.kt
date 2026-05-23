package com.flixclusive.data.backup.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BackupProvider(
    @ProtoNumber(1) val id: String,
    @ProtoNumber(2) val repositoryUrl: String,
    // ProtoNumber(3) and (4) are reserved for removed sortOrder and isEnabled fields
    @ProtoNumber(5) val createdAt: Long,
    @ProtoNumber(6) val updatedAt: Long,
    @ProtoNumber(7) val isCatalogEnabled: Boolean = true,
    @ProtoNumber(8) val isCrossMatchEnabled: Boolean = true,
    @ProtoNumber(9) val isMediaLinkEnabled: Boolean = true,
    @ProtoNumber(10) val isMetadataEnabled: Boolean = true,
    @ProtoNumber(11) val isSearchEnabled: Boolean = true,
    @ProtoNumber(12) val isTrackerEnabled: Boolean = true,
)
