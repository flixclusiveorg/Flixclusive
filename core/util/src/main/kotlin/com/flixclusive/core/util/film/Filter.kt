package com.flixclusive.core.util.film

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
     * @param options The list of values that can be selected.
     * @param state The current state, default is 0.
     */
    class Select<V>(
        val options: List<V>,
        state: Int? = null
    ) : Filter<Int?>("", state)

    /**
     * An abstract class representing a checkbox filter.
     *
     * @param name The name of the filter.
     * @param state The current state, default is false.
     */
    abstract class CheckBox(
        name: String,
        state: Boolean = false
    ) : Filter<Boolean>(name, state)

    /**
     * An abstract class representing a tri-state filter.
     *
     * @param name The name of the filter.
     * @param state The current state, default is [STATE_IGNORE].
     */
    abstract class TriState(
        name: String,
        state: Int? = null
    ) : Filter<Int?>(name, state) {
        fun isIgnored() = state == STATE_IGNORE
        fun isIncluded() = state == STATE_INCLUDE
        fun isExcluded() = state == STATE_EXCLUDE

        companion object {
            const val STATE_IGNORE = 0
            const val STATE_INCLUDE = 1
            const val STATE_EXCLUDE = 2
        }
    }

    /**
     * An abstract class representing a sort filter.
     *
     * @param name The name of the filter.
     * @param state The current state, which is a [Selection] object or null.
     * @property values The array of sortable values.
     */
    abstract class Sort(
        name: String,
        val values: List<String>,
        state: Selection? = null
    ) : Filter<Sort.Selection?>(name, state) {

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
