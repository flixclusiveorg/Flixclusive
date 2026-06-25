package com.flixclusive.core.database.entity.provider

import java.util.Date

sealed interface CachedMediaLink {
    val url: String
    val label: String
    val description: String?
    val customHeaders: Map<String, String>?
    val isDead: Boolean
    val providerId: String
    val ownerId: String
    val mediaId: String
    val episodeNumber: Int?
    val seasonNumber: Int?
    val createdAt: Date
    val updatedAt: Date

    val isValid: Boolean
        get() {
            return when (this) {
                is CachedStream -> !isDead && (expiresOn == null || expiresOn > System.currentTimeMillis())
                is CachedSubtitle -> !isDead
            }
        }
}
