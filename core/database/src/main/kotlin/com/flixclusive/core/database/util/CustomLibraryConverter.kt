package com.flixclusive.core.database.util

import androidx.room.TypeConverter
import com.flixclusive.model.database.LibraryItemId
import com.google.gson.Gson

internal class CustomLibraryConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromLibraryItemId(itemId: LibraryItemId): String = itemId.toString()

    @TypeConverter
    fun toLibraryItemId(value: String): LibraryItemId = LibraryItemId.fromString(value)
}
