package com.flixclusive.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * An entity representing a user.
 *
 * @property id The unique identifier of the user.
 * @property name The name of the user.
 * @property image The index of the image associated with the user.
 * @property pin The pin associated with the user.
 * */
@Entity
data class User(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("userId")
    val id: Int,
    val name: String,
    val image: Int,
    val pin: String = "",
) : Serializable