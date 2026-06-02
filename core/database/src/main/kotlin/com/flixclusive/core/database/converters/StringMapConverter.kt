package com.flixclusive.core.database.converters

import androidx.room.TypeConverter
import org.json.JSONObject

internal class StringMapConverter {
    @TypeConverter
    fun fromMap(value: Map<String, String>?): String? {
        return value?.let { JSONObject(it).toString() }
    }

    @TypeConverter
    fun toMap(value: String?): Map<String, String>? {
        return value?.let {
            val obj = JSONObject(it)
            buildMap {
                for (key in obj.keys()) {
                    put(key, obj.getString(key))
                }
            }
        }
    }
}
