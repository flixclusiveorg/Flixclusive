package com.flixclusive.feature.mobile.provider.test

import android.content.Context
import com.flixclusive.core.locale.R as LocaleR

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
                Name -> context.getString(LocaleR.string.sort_name)
                Date -> context.getString(LocaleR.string.sort_date)
                Score -> context.getString(LocaleR.string.sort_score)
            }
        }
    }
}