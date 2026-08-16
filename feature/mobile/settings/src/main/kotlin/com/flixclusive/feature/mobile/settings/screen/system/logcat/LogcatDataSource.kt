package com.flixclusive.feature.mobile.settings.screen.system.logcat

import android.os.SystemClock
import com.flixclusive.core.common.dispatchers.AppDispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

internal class LogcatDataSource @Inject constructor(
    private val appDispatchers: AppDispatchers,
) {
    val ownPid: Int = android.os.Process.myPid()

    /** Kept across restarts of [stream] rather than reset per process. Ids seed the list keys and
     * the clear marker, both of which break if a restarted stream starts numbering from zero again. */
    private val sequence = AtomicLong(0L)

    /**
     * `readLine` is an uninterruptible blocking call, so the reading loop cannot be cancelled from
     * the outside. [channelFlow] is what makes cancellation work: the builder body parks on the
     * cancellable `join`, and unwinding it destroys the process, which closes the pipe and drops the
     * blocked read out with EOF. A plain `flow { }` would leave both the process and its thread
     * behind forever.
     */
    fun stream(): Flow<List<LogEntry>> =
        channelFlow {
            val process = startProcess()
            val pump = launch(appDispatchers.io) { pump(process) }

            try {
                pump.join()
            } finally {
                process.destroy()
            }
        }

    private fun startProcess(): Process =
        try {
            newProcess(seeded = true)
        } catch (_: IOException) {
            newProcess(seeded = false)
        }

    private fun newProcess(seeded: Boolean): Process {
        val command = buildList {
            add(LOGCAT)
            add("-v")
            add("threadtime")
            if (seeded) {
                add("-T")
                add(SEED_LINES.toString())
            }
        }

        return ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
    }

    private suspend fun ProducerScope<List<LogEntry>>.pump(process: Process) {
        var previous: LogEntry? = null
        val batch = ArrayList<LogEntry>(MAX_BATCH)
        var lastFlush = SystemClock.uptimeMillis()

        try {
            process.inputStream.bufferedReader().buffered(READ_BUFFER_BYTES).use { reader ->
                while (true) {
                    currentCoroutineContext().ensureActive()

                    if (batch.shouldFlush(reader, lastFlush)) {
                        send(ArrayList(batch))
                        batch.clear()
                        lastFlush = SystemClock.uptimeMillis()
                    }

                    val raw = reader.readLine() ?: break
                    val entry = LogcatParser.parse(
                        raw = raw,
                        id = sequence.get(),
                        previous = previous,
                    )

                    if (entry != null) {
                        previous = entry
                        sequence.incrementAndGet()
                        batch += entry
                    }
                }

                if (batch.isNotEmpty()) send(ArrayList(batch))
            }
        } catch (exception: IOException) {
            // The stream dies with an IOException when the process is destroyed on cancellation,
            // which is the normal shutdown path rather than a failure worth surfacing.
            currentCoroutineContext().ensureActive()
            throw exception
        } catch (exception: CancellationException) {
            throw exception
        }
    }

    /**
     * `ready()` does the coalescing for free: a burst piles into one batch while an idle stream
     * flushes its tail straight away. The ceiling keeps the list moving during a sustained torrent.
     */
    private fun List<LogEntry>.shouldFlush(
        reader: BufferedReader,
        lastFlush: Long,
    ): Boolean {
        if (isEmpty()) return false
        if (size >= MAX_BATCH) return true

        val elapsed = SystemClock.uptimeMillis() - lastFlush
        if (elapsed >= MAX_FLUSH_MS) return true

        return elapsed >= MIN_FLUSH_MS && !reader.ready()
    }

    private companion object {
        const val LOGCAT = "logcat"
        const val SEED_LINES = 500
        const val MIN_FLUSH_MS = 120L
        const val MAX_FLUSH_MS = 250L
        const val MAX_BATCH = 300
        const val READ_BUFFER_BYTES = 32 * 1024
    }
}
