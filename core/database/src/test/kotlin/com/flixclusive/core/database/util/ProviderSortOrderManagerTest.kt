package com.flixclusive.core.database.util

import com.flixclusive.core.database.dao.provider.InstalledProviderDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isTrue

class ProviderSortOrderManagerTest {
    private lateinit var dao: InstalledProviderDao
    private lateinit var manager: ProviderSortOrderManager

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        manager = ProviderSortOrderManager(dao)
    }

    @Test
    fun `getNextSortOrder returns step when no providers exist`() = runTest {
        coEvery { dao.getMaxSortOrder() } returns null
        val result = manager.getNextSortOrder()
        expectThat(result).isEqualTo(ProviderSortOrderManager.NORMALIZATION_STEP)
    }

    @Test
    fun `getNextSortOrder returns max plus step`() = runTest {
        coEvery { dao.getMaxSortOrder() } returns 5.0
        val result = manager.getNextSortOrder()
        expectThat(result).isEqualTo(6.0)
    }

    @Test
    fun `computeMidpoint between two values`() {
        val result = manager.computeMidpoint(before = 1.0, after = 3.0)
        expectThat(result).isEqualTo(2.0)
    }

    @Test
    fun `computeMidpoint with null before inserts at top`() {
        val result = manager.computeMidpoint(before = null, after = 2.0)
        expectThat(result).isEqualTo(1.0) // (0.0 + 2.0) / 2
    }

    @Test
    fun `computeMidpoint with null after inserts at bottom`() {
        val result = manager.computeMidpoint(before = 3.0, after = null)
        expectThat(result).isEqualTo(2.5) // (3.0 + 4.0) / 2
    }

    @Test
    fun `computeMidpoint with both nulls`() {
        val result = manager.computeMidpoint(before = null, after = null)
        expectThat(result).isEqualTo(0.5) // (0.0 + 1.0) / 2
    }

    @Test
    fun `needsRenormalization returns false for large gap`() {
        expectThat(manager.needsRenormalization(1.0, 2.0)).isFalse()
    }

    @Test
    fun `needsRenormalization returns true for tiny gap`() {
        expectThat(manager.needsRenormalization(1.0, 1.0005)).isTrue()
    }

    @Test
    fun `needsRenormalization returns false at boundary`() {
        expectThat(manager.needsRenormalization(1.0, 1.002)).isFalse()
    }

    @Test
    fun `needsRenormalization with null before`() {
        expectThat(manager.needsRenormalization(null, 0.0005)).isTrue()
    }

    @Test
    fun `renormalize assigns evenly spaced values`() = runTest {
        val ids = listOf("a", "b", "c")
        manager.renormalize(ids)

        coVerify { dao.updateSortOrder(id = "a", sortOrder = 1.0) }
        coVerify { dao.updateSortOrder(id = "b", sortOrder = 2.0) }
        coVerify { dao.updateSortOrder(id = "c", sortOrder = 3.0) }
    }

    @Test
    fun `reorder does nothing when fromIndex equals toIndex`() = runTest {
        val ids = listOf("a", "b", "c")
        val orders = listOf(1.0, 2.0, 3.0)

        val result = manager.reorder(ids, orders, fromIndex = 1, toIndex = 1)
        expectThat(result).isEqualTo(ids)
    }

    @Test
    fun `reorder moves item and computes midpoint`() = runTest {
        val ids = listOf("a", "b", "c")
        val orders = listOf(1.0, 2.0, 3.0)

        // Move "c" (index 2) to index 1 (between "a" and "b")
        val result = manager.reorder(ids, orders, fromIndex = 2, toIndex = 1)

        expectThat(result).isEqualTo(listOf("a", "c", "b"))
        // Midpoint between 1.0 (a) and 2.0 (b) = 1.5
        coVerify { dao.updateSortOrder(id = "c", sortOrder = 1.5) }
    }

    @Test
    fun `reorder triggers renormalization when gap is too small`() = runTest {
        val ids = listOf("a", "b", "c")
        val orders = listOf(1.0, 1.0002, 1.0004)

        // Move "c" (index 2) to index 1 — gap between 1.0 and 1.0002 is < MIN_GAP
        val result = manager.reorder(ids, orders, fromIndex = 2, toIndex = 1)

        expectThat(result).isEqualTo(listOf("a", "c", "b"))
        // Should renormalize all three
        coVerify { dao.updateSortOrder(id = "a", sortOrder = 1.0) }
        coVerify { dao.updateSortOrder(id = "c", sortOrder = 2.0) }
        coVerify { dao.updateSortOrder(id = "b", sortOrder = 3.0) }
    }

    @Test
    fun `reorder moves item to top`() = runTest {
        val ids = listOf("a", "b", "c")
        val orders = listOf(1.0, 2.0, 3.0)

        // Move "c" (index 2) to index 0 (top)
        val result = manager.reorder(ids, orders, fromIndex = 2, toIndex = 0)

        expectThat(result).isEqualTo(listOf("c", "a", "b"))
        // before=null, after=1.0 → midpoint = (0.0 + 1.0) / 2 = 0.5
        coVerify { dao.updateSortOrder(id = "c", sortOrder = 0.5) }
    }

    @Test
    fun `reorder moves item to bottom`() = runTest {
        val ids = listOf("a", "b", "c")
        val orders = listOf(1.0, 2.0, 3.0)

        // Move "a" (index 0) to index 2 (bottom)
        val result = manager.reorder(ids, orders, fromIndex = 0, toIndex = 2)

        expectThat(result).isEqualTo(listOf("b", "c", "a"))
        // before=3.0, after=null → midpoint = (3.0 + 4.0) / 2 = 3.5
        coVerify { dao.updateSortOrder(id = "a", sortOrder = 3.5) }
    }

    @Test
    fun `successive midpoints maintain ordering`() {
        // Simulate multiple insertions between same neighbors
        var low = 1.0
        val high = 2.0
        for (i in 0 until 10) {
            val mid = manager.computeMidpoint(low, high)
            expectThat(mid).isGreaterThan(low)
            expectThat(mid).isLessThan(high)
            low = mid
        }
    }
}
