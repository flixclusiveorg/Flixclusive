package com.flixclusive.data.watch_history.di

import com.flixclusive.data.watch_history.WatchHistoryRepository
import io.mockk.coEvery
import io.mockk.mockk

object TestWatchHistoryDataModule {
    fun getMockWatchHistoryRepository(): WatchHistoryRepository {
        return mockk<WatchHistoryRepository> {
            coEvery {
                getRandomWatchHistoryItems(count = 1)
            } returns emptyList()
        }
    }
}