package com.flixclusive.data.backup.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BackupProviderRepository(
    @ProtoNumber(1) val url: String,
    @ProtoNumber(2) val owner: String,
    @ProtoNumber(3) val name: String,
    @ProtoNumber(4) val rawLinkFormat: String,
    @ProtoNumber(5) val createdAt: Long,
    @ProtoNumber(6) val updatedAt: Long,
)
