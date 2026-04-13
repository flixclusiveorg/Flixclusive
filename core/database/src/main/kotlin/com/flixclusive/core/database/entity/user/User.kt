package com.flixclusive.core.database.entity.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.Date

@Entity
data class User(
    @PrimaryKey
    @ColumnInfo("userId")
    val id: String,
    val name: String,
    val image: Int,
    val pin: String? = null,
    val pinHint: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
) : Serializable {
    companion object {
        const val MAX_USER_PIN_LENGTH = 4

        val Empty = User("", "", 0)
    }
}
