package com.flixclusive.core.database.entity.provider

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date

@Entity(
    tableName = "cached_streams",
    primaryKeys = ["url", "parentId"],
    foreignKeys = [
        ForeignKey(
            entity = CachedMediaLinks::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["parentId"]),
    ],
)
data class DBStream(
    override val url: String,
    override val parentId: String,
    override val label: String,
    override val description: String? = null,
    override val customHeaders: Map<String, String>? = null,
    override val isDead: Boolean = false,
    override val createdAt: Date = Date(),
    override val updatedAt: Date = Date(),
    val expiresOn: Long? = null,
    val isThirdPartyGateway: Boolean = false,
    val thirdPartyGatewayName: String? = null,
    val thirdPartyGatewayLogo: String? = null,
) : DBMediaLink {
    val isValid: Boolean
        get() = !isDead && (expiresOn == null || expiresOn > System.currentTimeMillis())
}
