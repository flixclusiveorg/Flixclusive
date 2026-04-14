package com.flixclusive.data.backup.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BackupProvider(
    @ProtoNumber(1) val id: String,
    @ProtoNumber(2) val repositoryUrl: String,
    @ProtoNumber(3) val sortOrder: Double,
    @ProtoNumber(4) val isEnabled: Boolean,
    @ProtoNumber(5) val createdAt: Long,
    @ProtoNumber(6) val updatedAt: Long,
)
