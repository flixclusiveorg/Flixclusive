package com.flixclusive.crash

import com.flixclusive.core.common.exception.CrashReportSender
import com.flixclusive.core.util.network.okhttp.HttpMethod
import com.flixclusive.core.util.network.okhttp.formRequest
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isTrue

private const val REMOTE_FORM_URL =
    "https://docs.google.com/forms/u/0/d/e/1FAIpQLSfTVmgiOeF7RlDbjBR10RQG6C6uKioSk-toqKecPvpkAe9ffw/formResponse?pli=1"

class CrashReportSenderTest : CrashReportSender {
    private lateinit var client: OkHttpClient

    private fun generateDirtyClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    @Before
    fun setUp() {
        client = generateDirtyClient()
    }

    @Test
    fun `send function should return true`() = runTest {
        val errorMessage = """
            ========== ERROR-SAMPLE ===========

            LOREM IPSUM LOREM IPSUM LOREM IPSUM
            LOREM IPSUM LOREM IPSUM LOREM IPSUM
            LOREM IPSUM LOREM IPSUM LOREM IPSUM
            LOREM IPSUM LOREM IPSUM LOREM IPSUM
            ===================================
        """.trimIndent()

        send(errorMessage)
    }

    override suspend fun send(errorLog: String) {
        val response = client.formRequest(
            url = REMOTE_FORM_URL,
            method = HttpMethod.POST,
            body = mapOf("entry.1687138646" to errorLog)
        ).execute()

        expectThat(response.isSuccessful).isTrue()
    }
}
