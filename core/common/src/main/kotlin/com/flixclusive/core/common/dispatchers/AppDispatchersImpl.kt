package com.flixclusive.core.common.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [AppDispatchers] that provides the standard
 * Kotlin coroutine dispatchers.
 */
@Singleton
internal class AppDispatchersImpl @Inject constructor() : AppDispatchers {
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined

    override val ioScope get() = CoroutineScope(io + SupervisorJob())
    override val defaultScope get() = CoroutineScope(default + SupervisorJob())
    override val mainScope get() = CoroutineScope(main + SupervisorJob())
}
