package com.flixclusive.domain.model.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("userId")
    val id: Int = 1,
    val name: String = USER_DEFAULT_NAME,
    val image: Int = 0
) {
    companion object {
        private const val USER_DEFAULT_NAME = "User"
    }
}