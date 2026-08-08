package com.flixclusive.data.downloads.transfer

import android.system.ErrnoException
import android.system.OsConstants
import java.io.IOException

/**
 * Why a transfer failed, at the only granularity the caller actually acts on: is this link worth
 * giving up on, or is the device the problem?
 *
 * The distinction matters because failing a download marks its source link dead and moves to the
 * next candidate. When the real cause is a full disk or a dropped connection, doing that walks
 * through and blacklists every cached link for a title in seconds — so a retry, once there is space
 * or signal again, has nothing left to try.
 */
enum class TransferFailure {
    /** The link is at fault — dead host, bad status, truncated body. Worth trying another. */
    LINK,

    /** The device or its connection is at fault. Trying another link would fail the same way. */
    ENVIRONMENT,

    ;

    companion object {
        fun of(cause: Throwable?): TransferFailure {
            // Walk the chain: OkHttp and the SAF layer both wrap the interesting exception. Bounded
            // rather than followed to the end, because a cause chain is allowed to be cyclic and an
            // unbounded walk would hang on one.
            var current: Throwable? = cause
            repeat(MAX_CAUSE_DEPTH) {
                val throwable = current ?: return LINK
                if (throwable.isEnvironmentFault()) return ENVIRONMENT
                current = throwable.cause
            }

            return LINK
        }

        private fun Throwable.isEnvironmentFault(): Boolean =
            when {
                // Revoked SAF permission, or a document URI that is no longer ours to write.
                this is SecurityException -> true
                this is ErrnoException -> errno == OsConstants.ENOSPC || errno == OsConstants.EDQUOT
                // A full volume surfaces as a plain IOException with this text on most Android
                // versions rather than as a typed exception.
                this is IOException && message?.contains(NO_SPACE_MESSAGE, ignoreCase = true) == true -> true
                else -> false
            }

        private const val NO_SPACE_MESSAGE = "No space left on device"

        /** Deep enough for any real wrapping, shallow enough to survive a cyclic chain. */
        private const val MAX_CAUSE_DEPTH = 10
    }
}
