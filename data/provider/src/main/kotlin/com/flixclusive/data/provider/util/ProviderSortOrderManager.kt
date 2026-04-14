package com.flixclusive.data.provider.util

import com.flixclusive.core.database.dao.provider.InstalledProviderDao
import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.data.provider.util.ProviderSortOrderManager.Companion.MIN_GAP

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
    suspend fun getNextSortOrder(ownerId: String): Double {
        val maxOrder = installedProviderDao.getMaxSortOrder(ownerId) ?: 0.0
        return maxOrder + NORMALIZATION_STEP
    }

    private fun computeMidpoint(before: Double?, after: Double?): Double {
        val low = before ?: 0.0
        val high = after ?: (low + NORMALIZATION_STEP)
        return (low + high) / 2.0
    }

    fun needsRenormalization(all: List<InstalledProvider>): Boolean {
        if (all.size < 2) return false
        return all.zipWithNext().any { (a, b) ->
            b.sortOrder - a.sortOrder < MIN_GAP
        }
    }

    suspend fun renormalize(orderedIds: List<InstalledProvider>, ownerId: String) {
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
        moved: InstalledProvider,
        before: InstalledProvider?,
        after: InstalledProvider?,
    ) {
        installedProviderDao.updateSortOrder(
            id = moved.id,
            ownerId = moved.ownerId,
            sortOrder = computeMidpoint(before?.sortOrder, after?.sortOrder)
        )
    }

    companion object {
        const val MIN_GAP = 1e-10
        const val NORMALIZATION_STEP = 1.0
    }
}
