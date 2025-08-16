package com.flixclusive.domain.database.usecase.impl

import com.flixclusive.core.database.entity.watched.WatchProgress
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.domain.database.usecase.SetWatchProgressUseCase
import javax.inject.Inject

internal class SetWatchProgressUseCaseImpl
    @Inject
    constructor(
        private val watchProgressRepository: WatchProgressRepository,
    ) : SetWatchProgressUseCase {
        override suspend fun invoke(watchProgress: WatchProgress) {
            if (watchProgress.isLessThanAMinute()) {
                return
            }

            watchProgressRepository.insert(watchProgress)
        }
    }
