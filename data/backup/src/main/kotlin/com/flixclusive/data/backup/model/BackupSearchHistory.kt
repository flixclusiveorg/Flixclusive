package com.flixclusive.data.backup.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BackupSearchHistory(
    @ProtoNumber(1) val query: String,
    @ProtoNumber(3) val createdAt: Long,
    @ProtoNumber(4) val updatedAt: Long,
)
