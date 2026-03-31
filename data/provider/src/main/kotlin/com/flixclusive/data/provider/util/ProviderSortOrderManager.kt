package com.flixclusive.data.provider.util

import com.flixclusive.core.database.dao.provider.InstalledProviderDao
import com.flixclusive.core.database.entity.provider.InstalledProvider

/**
 * Manages provider sort ordering using a midpoint / Lexorank-style approach.
 *
 * To reorder a provider between two others, compute the midpoint of their `sortOrder` values.
 * If precision degrades (gap < [MIN_GAP]), trigger a re-normalization that reassigns
 * evenly spaced values (1.0, 2.0, 3.0...).
 * */
internal class ProviderSortOrderManager(
    private val installedProviderDao: InstalledProviderDao,
) {
    suspend fun getNextSortOrder(ownerId: Int): Double {
        val maxOrder = installedProviderDao.getMaxSortOrder(ownerId) ?: 0.0
        return maxOrder + NORMALIZATION_STEP
    }

    private fun computeMidpoint(before: Double?, after: Double?): Double {
        val low = before ?: 0.0
        val high = after ?: (low + NORMALIZATION_STEP)
        return (low + high) / 2.0
    }

    private fun needsRenormalization(before: Double?, after: Double?): Boolean {
        val low = before ?: 0.0
        val high = after ?: (low + NORMALIZATION_STEP)
        return (high - low) < MIN_GAP
    }

    private suspend fun renormalize(orderedIds: List<InstalledProvider>, ownerId: Int) {
        orderedIds.forEachIndexed { index, provider ->
            val newOrder = (index + 1) * NORMALIZATION_STEP
            installedProviderDao.updateSortOrder(
                id = provider.id,
                ownerId = ownerId,
                sortOrder = newOrder
            )
        }
    }

    suspend fun reorder(
        from: Int,
        to: Int,
        ownerId: Int,
    ) {
        if (from == to) return
        val currentOrderedList = installedProviderDao.getAll(ownerId = ownerId)

        val mutableOrders = currentOrderedList.toMutableList()
        val movedItem = mutableOrders.removeAt(from)
        mutableOrders.add(to, movedItem)

        val before = if (to > 0) mutableOrders[to - 1] else null
        val after = if (to < mutableOrders.size) mutableOrders[to] else null

        if (needsRenormalization(before?.sortOrder, after?.sortOrder)) {
            renormalize(mutableOrders, movedItem.ownerId)
        } else {
            val newOrder = computeMidpoint(before?.sortOrder, after?.sortOrder)
            installedProviderDao.updateSortOrder(
                id = movedItem.id,
                ownerId = movedItem.ownerId,
                sortOrder = newOrder
            )
        }
    }

    companion object {
        const val MIN_GAP = 0.001
        const val NORMALIZATION_STEP = 1.0
    }
}
