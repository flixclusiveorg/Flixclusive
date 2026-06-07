package com.flixclusive.domain.provider.usecase.links

import com.flixclusive.core.database.entity.provider.DBMediaLink
import kotlinx.coroutines.flow.Flow

sealed class TestLinksProgress {
    data class Testing(val url: String, val index: Int, val total: Int) : TestLinksProgress()
    data class Done(val aliveCount: Int, val deadCount: Int) : TestLinksProgress()
    data class Error(val cause: Throwable) : TestLinksProgress()
}

interface TestMediaLinksUseCase {
    operator fun invoke(links: List<DBMediaLink>): Flow<TestLinksProgress>
}
