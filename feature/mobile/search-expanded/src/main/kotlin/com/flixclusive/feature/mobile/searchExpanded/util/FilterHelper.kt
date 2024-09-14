package com.flixclusive.feature.mobile.searchExpanded.util

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.locale.UiText
import com.flixclusive.provider.filter.Filter
import com.flixclusive.provider.filter.FilterGroup

internal object FilterHelper {

    private const val SELECTED_FILTER_BUTTON_ALPHA = 0.05F

    fun FilterGroup.isBeingUsed(): Boolean {
        fastForEach { filter ->
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
            is Filter.Select<*>, is Filter.Sort<*> -> {
                return firstFilter.getFilterDisplayValue(context)
            }
            else -> name
        }
    }

    private fun FilterGroup.hasOneTypeOnly(): Boolean {
        if (isEmpty())
            return true

        val firstType = first()::class
        return fastAll { it::class == firstType }
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

    private fun Filter<*>.getFilterDisplayValue(context: Context): String {
        val selectedFilter = when (this) {
            is Filter.Select<*> -> options.getOrNull(state!!)
            is Filter.Sort<*> -> options.getOrNull(state!!.index)
            else -> null
        }

        return when (selectedFilter) {
            is UiText -> selectedFilter.asString(context)
            null -> name
            else -> selectedFilter.toString()
        }
    }
}