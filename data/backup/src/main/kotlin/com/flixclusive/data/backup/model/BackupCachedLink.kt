package com.flixclusive.data.backup.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
sealed interface BackupCachedLink {
    val url: String
    val label: String
    val description: String?
    val customHeaders: Map<String, String>?
    val isDead: Boolean
    val providerId: String
    val mediaId: String
    val episodeNumber: Int?
    val seasonNumber: Int?
    val createdAt: Long
    val updatedAt: Long
    val media: BackupDbMedia
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BackupCachedStream(
    @ProtoNumber(1) override val url: String,
    @ProtoNumber(2) override val label: String,
    @ProtoNumber(3) override val description: String? = null,
    @ProtoNumber(4) override val customHeaders: Map<String, String>? = null,
    @ProtoNumber(5) override val isDead: Boolean = false,
    @ProtoNumber(6) override val providerId: String,
    @ProtoNumber(8) override val mediaId: String,
    @ProtoNumber(9) override val episodeNumber: Int? = null,
    @ProtoNumber(10) override val seasonNumber: Int? = null,
    @ProtoNumber(11) override val createdAt: Long,
    @ProtoNumber(12) override val updatedAt: Long,
    @ProtoNumber(13) val expiresOn: Long? = null,
    @ProtoNumber(14) val isThirdPartyGateway: Boolean = false,
    @ProtoNumber(15) val thirdPartyGatewayName: String? = null,
    @ProtoNumber(16) val thirdPartyGatewayLogo: String? = null,
    @ProtoNumber(17) override val media: BackupDbMedia,
) : BackupCachedLink

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BackupCachedSubtitle(
    @ProtoNumber(1) override val url: String,
    @ProtoNumber(2) override val label: String,
    @ProtoNumber(3) override val description: String? = null,
    @ProtoNumber(4) override val customHeaders: Map<String, String>? = null,
    @ProtoNumber(5) override val isDead: Boolean = false,
    @ProtoNumber(6) override val providerId: String,
    @ProtoNumber(8) override val mediaId: String,
    @ProtoNumber(9) override val episodeNumber: Int? = null,
    @ProtoNumber(10) override val seasonNumber: Int? = null,
    @ProtoNumber(11) override val createdAt: Long,
    @ProtoNumber(12) override val updatedAt: Long,
    @ProtoNumber(13) val subtitleSource: String = "ONLINE",
    @ProtoNumber(14) override val media: BackupDbMedia,
) : BackupCachedLink
