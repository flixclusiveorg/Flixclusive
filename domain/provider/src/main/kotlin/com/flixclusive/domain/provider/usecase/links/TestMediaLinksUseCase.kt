package com.flixclusive.domain.provider.usecase.links

import kotlinx.coroutines.flow.Flow

/** Represents progress of a link liveness test. */
sealed class TestLinksProgress {
    data class Testing(val url: String, val index: Int, val total: Int) : TestLinksProgress()
    data class Done(val aliveCount: Int, val deadCount: Int) : TestLinksProgress()
    data class Error(val cause: Throwable) : TestLinksProgress()
}

/** Tests every non-dead stream in the cache entry identified by [id] with an HTTP HEAD request and marks each as dead/alive. */
interface TestMediaLinksUseCase {
    operator fun invoke(id: String): Flow<TestLinksProgress>
}
