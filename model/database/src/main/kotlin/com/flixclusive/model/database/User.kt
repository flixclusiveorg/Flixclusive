package com.flixclusive.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

private const val USER_DEFAULT_NAME = "User"

/**
 * An entity representing a user.
 *
 * @property id The unique identifier of the user.
 * @property name The name of the user.
 * @property image The index of the image associated with the user.
 * */
@Entity
data class User(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("userId")
    val id: Int = 1,
    val name: String = USER_DEFAULT_NAME,
    val image: Int = 0
    // TODO: Add pin column
)