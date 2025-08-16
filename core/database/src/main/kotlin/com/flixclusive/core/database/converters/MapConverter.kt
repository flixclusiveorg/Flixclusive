package com.flixclusive.core.database.converters

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class MapConverter {
    @TypeConverter
    fun fromMap(customProperties: Map<String, String?>): String {
        return Json.encodeToString(customProperties)
    }

    @TypeConverter
    fun toMap(filmDataString: String): Map<String, String?> {
        return Json.decodeFromString(filmDataString)
    }
}
