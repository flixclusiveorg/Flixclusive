package com.flixclusive.core.database.entity.provider

import java.util.Date

sealed interface DBMediaLink {
    val url: String
    val label: String
    val description: String?
    val parentId: String
    val customHeaders: Map<String, String>?
    val isDead: Boolean
    val createdAt: Date
    val updatedAt: Date

    val isValid: Boolean
        get() {
            return when (this) {
                is DBStream -> !isDead && (expiresOn == null || expiresOn > System.currentTimeMillis())
                is DBSubtitle -> !isDead
            }
        }
}
