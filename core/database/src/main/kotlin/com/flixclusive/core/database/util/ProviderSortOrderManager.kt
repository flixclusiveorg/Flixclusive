package com.flixclusive.core.database.util

import com.flixclusive.core.database.dao.provider.InstalledProviderDao
import com.flixclusive.core.database.util.ProviderSortOrderManager.Companion.MIN_GAP
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages provider sort ordering using a midpoint / Lexorank-style approach.
 *
 * To reorder a provider between two others, compute the midpoint of their `sortOrder` values.
 * If precision degrades (gap < [MIN_GAP]), trigger a re-normalization that reassigns
 * evenly spaced values (1.0, 2.0, 3.0...).
 * */
@Singleton
class ProviderSortOrderManager @Inject constructor(
    private val installedProviderDao: InstalledProviderDao,
) {
    /**
     * Compute the sort order for inserting a new provider at the end of the list.
     * */
    suspend fun getNextSortOrder(): Double {
        val maxOrder = installedProviderDao.getMaxSortOrder() ?: 0.0
        return maxOrder + NORMALIZATION_STEP
    }

    /**
     * Compute the midpoint sort order for inserting between two neighbors.
     *
     * @param before The sort order of the item above (or null if inserting at the top).
     * @param after The sort order of the item below (or null if inserting at the bottom).
     * @return The computed sort order.
     * */
    fun computeMidpoint(before: Double?, after: Double?): Double {
        val low = before ?: 0.0
        val high = after ?: (low + NORMALIZATION_STEP)
        return (low + high) / 2.0
    }

    /**
     * Checks whether a re-normalization is needed (gap between neighbors is too small).
     * */
    fun needsRenormalization(before: Double?, after: Double?): Boolean {
        val low = before ?: 0.0
        val high = after ?: (low + NORMALIZATION_STEP)
        return (high - low) < MIN_GAP
    }

    /**
     * Re-normalizes all provider sort orders by reassigning evenly spaced values.
     * The relative order is preserved.
     *
     * @param orderedIds The list of provider IDs in their current display order.
     * */
    suspend fun renormalize(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id ->
            val newOrder = (index + 1) * NORMALIZATION_STEP
            installedProviderDao.updateSortOrder(id = id, sortOrder = newOrder)
        }
    }

    /**
     * Reorders a provider to a new position.
     *
     * @param currentOrderedIds Full list of provider IDs in their current display order.
     * @param fromIndex The current index of the item being moved.
     * @param toIndex The target index.
     * @return The updated list of provider IDs in display order.
     * */
    suspend fun reorder(
        currentOrderedIds: List<String>,
        currentSortOrders: List<Double>,
        fromIndex: Int,
        toIndex: Int,
    ): List<String> {
        if (fromIndex == toIndex) return currentOrderedIds

        val mutableIds = currentOrderedIds.toMutableList()
        val movedId = mutableIds.removeAt(fromIndex)
        mutableIds.add(toIndex, movedId)

        val mutableOrders = currentSortOrders.toMutableList()
        mutableOrders.removeAt(fromIndex)

        val before = if (toIndex > 0) mutableOrders[toIndex - 1] else null
        val after = if (toIndex < mutableOrders.size) mutableOrders[toIndex] else null

        if (needsRenormalization(before, after)) {
            renormalize(mutableIds)
        } else {
            val newOrder = computeMidpoint(before, after)
            installedProviderDao.updateSortOrder(id = movedId, sortOrder = newOrder)
        }

        return mutableIds
    }

    companion object {
        const val MIN_GAP = 0.001
        const val NORMALIZATION_STEP = 1.0
    }
}
