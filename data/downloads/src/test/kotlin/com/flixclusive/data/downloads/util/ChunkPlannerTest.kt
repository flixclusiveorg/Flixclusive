package com.flixclusive.data.downloads.util

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThanOrEqualTo

class ChunkPlannerTest {
    @Test
    fun `plan should return a single open-ended chunk when total bytes is null`() {
        val result = ChunkPlanner.plan(null)

        expectThat(result).hasSize(1)
        expectThat(result[0].first).isEqualTo(0L)
        expectThat(result[0].last).isEqualTo(ChunkPlanner.OPEN_ENDED_RANGE_END)
    }

    @Test
    fun `plan should return a single open-ended chunk when total bytes is zero or negative`() {
        expectThat(ChunkPlanner.plan(0L)).hasSize(1)
        expectThat(ChunkPlanner.plan(-5L)).hasSize(1)
    }

    @Test
    fun `plan should return a single chunk when total bytes is smaller than the minimum chunk size`() {
        val result = ChunkPlanner.plan(1L * 1024 * 1024)

        expectThat(result).hasSize(1)
        expectThat(result[0].first).isEqualTo(0L)
        expectThat(result[0].last).isEqualTo(1L * 1024 * 1024 - 1)
    }

    @Test
    fun `plan should cap chunk count at the maximum even for very large files`() {
        val result = ChunkPlanner.plan(1024L * 1024 * 1024)

        expectThat(result).hasSize(4)
    }

    @Test
    fun `plan should produce contiguous ranges covering the whole file exactly`() {
        val totalBytes = 9L * 1024 * 1024
        val result = ChunkPlanner.plan(totalBytes)

        expectThat(result.first().first).isEqualTo(0L)
        expectThat(result.last().last).isEqualTo(totalBytes - 1)

        for (i in 0 until result.size - 1) {
            expectThat(result[i].last + 1).isEqualTo(result[i + 1].first)
        }
    }

    @Test
    fun `plan should keep each chunk at or above the minimum chunk size when splitting`() {
        val totalBytes = 5L * 1024 * 1024
        val result = ChunkPlanner.plan(totalBytes)

        expectThat(result).hasSize(2)
        result.forEach { range ->
            expectThat(range.last - range.first + 1).isGreaterThanOrEqualTo(2L * 1024 * 1024)
        }
    }
}
