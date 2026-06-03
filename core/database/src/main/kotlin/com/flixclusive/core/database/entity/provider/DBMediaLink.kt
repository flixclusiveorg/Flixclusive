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
}
