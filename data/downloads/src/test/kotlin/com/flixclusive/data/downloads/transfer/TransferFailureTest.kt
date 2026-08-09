package com.flixclusive.data.downloads.transfer

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.IOException

class TransferFailureTest {
    @Test
    fun `a plain transport error is the link's fault`() {
        expectThat(TransferFailure.of(IOException("Chunk request failed: 404")))
            .isEqualTo(TransferFailure.LINK)
    }

    @Test
    fun `a null cause is treated as the link's fault`() {
        // Nothing to go on, and LINK keeps the existing try-the-next-candidate behaviour.
        expectThat(TransferFailure.of(null)).isEqualTo(TransferFailure.LINK)
    }

    @Test
    fun `a full disk is the environment's fault`() {
        expectThat(TransferFailure.of(IOException("write failed: No space left on device")))
            .isEqualTo(TransferFailure.ENVIRONMENT)
    }

    @Test
    fun `a revoked folder permission is the environment's fault`() {
        expectThat(TransferFailure.of(SecurityException("Permission Denial: opening provider")))
            .isEqualTo(TransferFailure.ENVIRONMENT)
    }

    @Test
    fun `an environment fault wrapped by a transport error is still the environment's fault`() {
        // OkHttp and the SAF layer both wrap, so the classifier has to walk the chain.
        val wrapped = IOException("Chunk 0 failed", IOException("No space left on device"))

        expectThat(TransferFailure.of(wrapped)).isEqualTo(TransferFailure.ENVIRONMENT)
    }

    @Test
    fun `a cyclic cause chain terminates instead of hanging`() {
        // Cause chains are allowed to be cyclic; an unbounded walk would spin here forever.
        val inner = IOException("inner")
        val outer = IOException("outer", inner)
        inner.initCause(outer)

        expectThat(TransferFailure.of(outer)).isEqualTo(TransferFailure.LINK)
    }
}
