package com.flixclusive.core.util.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

enum class AppDispatchers(
    val dispatcher: CoroutineDispatcher,
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
) {
    Default(Dispatchers.Default),
    IO(Dispatchers.IO),
    Main(Dispatchers.Main);

    companion object {
        /**
         * This uses withContext([Dispatchers.IO]) for suspend functions.
         *
         * @param block The suspend function to run.
         *
         * @see runOnIO
         * @see launchOnIO
         * */
        suspend inline fun <T> withIOContext(
            crossinline block: suspend () -> T
        ): T = withContext(IO.dispatcher) {
            block()
        }

        /**
         * This uses withContext([Dispatchers.Default]) for suspend functions.
         *
         * @param block The suspend function to run.
         *
         * @see runOnDefault
         * @see launchOnDefault
         * */
        suspend inline fun <T> withDefaultContext(
            crossinline block: suspend () -> T
        ): T = withContext(Default.dispatcher) {
            block()
        }

        /**
         * This uses withContext([Dispatchers.Main]) for suspend functions.
         *
         * @param block The suspend function to run.
         *
         * @see runOnMain
         * @see launchOnMain
         * */
        suspend inline fun <T> withMainContext(
            crossinline block: suspend () -> T
        ): T = withContext(Main.dispatcher) {
            block()
        }

        /**
         * This uses runBlocking([Dispatchers.IO]) for **non** suspend functions.
         *
         * @param block The suspend function to run.
         *
         * @see withIOContext
         * @see launchOnIO
         * */
        inline fun <T> runOnIO(
            crossinline block: suspend () -> T
        ): T = runBlocking(IO.dispatcher) {
            block()
        }

        /**
         * This uses runBlocking([Dispatchers.Default]) for **non** suspend functions.
         *
         * @param block The suspend function to run.
         *
         * @see withDefaultContext
         * @see launchOnDefault
         * */
        inline fun <T> runOnDefault(
            crossinline block: suspend () -> T
        ): T = runBlocking(Default.dispatcher) {
            block()
        }

        /**
         * This uses runBlocking([Dispatchers.Main]) for **non** suspend functions.
         *
         * @param block The suspend function to run.
         *
         * @see withMainContext
         * @see launchOnMain
         * */
        inline fun <T> runOnMain(
            crossinline block: suspend () -> T
        ): T = runBlocking(Main.dispatcher) {
            block()
        }

        /**
         * Launches a new coroutine on the [Dispatchers.IO] context and executes the given block.
         *
         * @param block The suspend function to run.
         *
         * @see withIOContext
         * @see runOnIO
         */
        inline fun launchOnIO(crossinline block: suspend () -> Unit) {
            IO.scope.launch {
                block()
            }
        }

        /**
         * Launches a new coroutine on the [Dispatchers.Default] context and executes the given block.
         *
         * @param block The suspend function to run.
         *
         * @see withDefaultContext
         * @see runOnDefault
         */
        inline fun launchOnDefault(crossinline block: suspend () -> Unit) {
            Default.scope.launch {
                block()
            }
        }

        /**
         * Launches a new coroutine on the [Dispatchers.Main] context and executes the given block.
         *
         * @param block The suspend function to run.
         *
         * @see withMainContext
         * @see runOnMain
         */
        inline fun launchOnMain(crossinline block: suspend () -> Unit) {
            Main.scope.launch {
                block()
            }
        }
    }
}