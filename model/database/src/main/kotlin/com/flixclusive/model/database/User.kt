package com.flixclusive.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

const val MAX_USER_PIN_LENGTH = 4

/**
 * An entity representing a user.
 *
 * @property id The unique identifier of the user.
 * @property name The name of the user.
 * @property image The index of the image associated with the user.
 * @property pin The pin associated with the user.
 * @property pinHint The hint for the user's pin. Required for PIN-based authentication.
 * */
@Entity
data class User(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("userId")
    val id: Int,
    val name: String,
    val image: Int,
    val pin: String? = null,
    val pinHint: String? = null,
) : Serializable