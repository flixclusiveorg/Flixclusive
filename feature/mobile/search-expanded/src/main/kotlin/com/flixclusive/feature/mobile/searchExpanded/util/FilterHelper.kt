package com.flixclusive.feature.mobile.searchExpanded.util

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.core.util.film.Filter
import com.flixclusive.core.util.film.FilterGroup

internal object FilterHelper {

    private const val SELECTED_FILTER_BUTTON_ALPHA = 0.05F

    fun FilterGroup.isBeingUsed(): Boolean {
        for (filter in this) {
            if (filter is Filter.CheckBox && filter.state) {
                return true
            }

            if (filter.state != null)
                return true
        }

        return false
    }

    fun FilterGroup.getFormattedName(context: Context): String {
        if (isEmpty() || !hasOneTypeOnly())
            return name

        val firstFilter = first()

        if (firstFilter.state == null)
            return name

        return when (firstFilter) {
            is Filter.CheckBox -> {
                val selected = count {
                    it is Filter.CheckBox && it.state
                }

                var groupName = firstFilter.name
                if (selected > 0) {
                    groupName += ", +$selected"
                }

                return groupName
            }
            is Filter.Select<*> -> {
                val selectedFilter = firstFilter.options
                    .getOrNull(firstFilter.state!!)

                if (selectedFilter is UiText) {
                    return selectedFilter.asString(context)
                }

                if (selectedFilter == null)
                    return name

                return selectedFilter.toString()
            }
            is Filter.Sort -> {
                val selectedFilter = firstFilter.values
                    .getOrNull(firstFilter.state!!.index)

                if (selectedFilter == null)
                    return name

                return selectedFilter
            }
            else -> name
        }
    }

    private fun FilterGroup.hasOneTypeOnly(): Boolean {
        if (isEmpty())
            return true

        val firstType = first()::class
        return all { it::class == firstType }
    }

    @Composable
    fun getButtonColors(isBeingUsed: Boolean): ButtonColors {
        return if (isBeingUsed) {
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primary.onMediumEmphasis(SELECTED_FILTER_BUTTON_ALPHA)
            )
        } else ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface.onMediumEmphasis(0.8F),
        )
    }

    @Composable
    fun getButtonBorders(isBeingUsed: Boolean): BorderStroke {
        return if (isBeingUsed) {
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else ButtonDefaults.outlinedButtonBorder
    }
}