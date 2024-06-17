package com.flixclusive.core.database.util

import androidx.room.TypeConverter
import java.util.Date

internal class DateConverter {
    @TypeConverter
    fun fromDate(value: Date): Long {
        return value.time
    }

    @TypeConverter
    fun toDate(value: Long): Date {
        return Date(value)
    }
}