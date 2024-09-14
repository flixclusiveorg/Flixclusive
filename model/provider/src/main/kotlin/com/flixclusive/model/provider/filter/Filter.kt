package com.flixclusive.model.provider.filter

import android.content.Context
import com.flixclusive.core.locale.UiText
import com.flixclusive.model.provider.filter.Filter.Sort.Selection
import com.flixclusive.model.provider.filter.Filter.TriState.Companion.STATE_UNSELECTED

/**
 * A sealed class representing a filter with a name and a state.
 *
 * This class originally came from Tachiyomi.
 *
 * @param name The name of the filter.
 * @param state The state of the filter.
 */
@Suppress("unused")
sealed class Filter<T>(
    val name: String,
    var state: T
) {
    /**
     * An abstract class representing a selectable filter.
     *
     * @param V The type of the values that can be selected.
     * @param name The name of the filter.
     * @param options The list of values that can be selected.
     * @param state The current state, default is 0.
     */
    open class Select<V>(
        name: String,
        val options: List<V>,
        state: Int = 0
    ) : Filter<Int>(name, state) {
        companion object {
            fun <T> getOptionName(
                option: T,
                context: Context
            ): String {
                return when (option) {
                    is com.flixclusive.core.locale.UiText -> option.asString(context)
                    else -> option.toString()
                }
            }
        }
    }

    /**
     * An abstract class representing a checkbox filter.
     *
     * @param name The name of the filter.
     * @param state The current state, default is false.
     */
    open class CheckBox(
        name: String,
        state: Boolean = false
    ) : Filter<Boolean>(name, state)

    /**
     * An abstract class representing a tri-state filter.
     *
     * It has three states: unselected, selected, and indeterminate.
     * - Unselected: The filter is not selected. Checkbox is empty.
     * - Selected: The filter is selected. Checkbox is checked.
     * - Indeterminate: The filter is indeterminate. Checkbox has a dash line.
     *
     * @param name The name of the filter.
     * @param state The current state, default is [STATE_UNSELECTED].
     */
    open class TriState(
        name: String,
        state: Int = STATE_UNSELECTED
    ) : Filter<Int>(name, state) {
        fun isUnselected() = state == STATE_UNSELECTED
        fun isSelected() = state == STATE_SELECTED
        fun isIndeterminate() = state == STATE_INDETERMINATE

        companion object {
            const val STATE_UNSELECTED = 0
            const val STATE_SELECTED = 1
            const val STATE_INDETERMINATE = 2
        }
    }

    /**
     * An abstract class representing a sort filter.
     *
     * @param name The name of the filter.
     * @property options The list of filters that could be sorted either on ascending or descending order.
     * @param state The current state, which is a [Selection] object or null.
     */
    open class Sort<V>(
        name: String,
        val options: List<V>,
        state: Selection? = null
    ) : Filter<Selection?>(name, state) {
        /**
         * A data class representing a selection with an index and a boolean indicating ascending order.
         *
         * @property index The index of the selection.
         * @property ascending True if the sorting is ascending, false if descending.
         */
        data class Selection(val index: Int, val ascending: Boolean)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Filter<*>) return false

        return name == other.name && state == other.state
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (state?.hashCode() ?: 0)
        return result
    }
}
