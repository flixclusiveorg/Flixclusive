package com.flixclusive.core.common.collections

/**
 * Utility object for sorting collections.
 * */
object SortUtils {
    /**
     * Returns a [Comparator] that compares two objects of type [T] based on a selected property.
     *
     * @param T The type of objects to be compared.
     * @param ascending A boolean indicating whether the comparison should be in ascending order.
     *                  If true, the comparison is in ascending order; if false, in descending order.
     *
     * @return A [Comparator] that compares two objects of type [T] based on the selected property.
     * */
    inline fun <T> compareBy(
        ascending: Boolean,
        crossinline selector: (T) -> Comparable<*>?,
    ): Comparator<T> =
        Comparator { a, b ->
            when {
                ascending -> compareValuesBy(a, b, selector)
                else -> compareValuesBy(b, a, selector)
            }
        }
}
