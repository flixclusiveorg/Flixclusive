package com.flixclusive.feature.mobile.provider.test

import android.content.Context
import com.flixclusive.core.util.R as UtilR

internal data class SortOption(
    val sort: SortType,
    val ascending: Boolean = true
) {
    enum class SortType {
        Name,
        Date,
        Score;

        fun toString(context: Context): String {
            return when (this) {
                Name -> context.getString(UtilR.string.sort_name)
                Date -> context.getString(UtilR.string.sort_date)
                Score -> context.getString(UtilR.string.sort_score)
            }
        }
    }
}