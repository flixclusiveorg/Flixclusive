package com.flixclusive.core.common.exception

import android.content.Context
import com.flixclusive.core.common.R
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.util.android.showToast
import com.flixclusive.core.util.network.okhttp.HttpMethod
import com.flixclusive.core.util.network.okhttp.formRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

private const val REMOTE_FORM_URL =
    "https://docs.google.com/forms/u/0/d/e/1FAIpQLSfTVmgiOeF7RlDbjBR10RQG6C6uKioSk-toqKecPvpkAe9ffw/formResponse?pli=1"

internal class CrashReportSenderImpl
    @Inject
    constructor(
        private val client: OkHttpClient,
        private val dispatchers: AppDispatchers,
        @ApplicationContext private val context: Context,
    ) : CrashReportSender {
        override suspend fun send(errorLog: String) {
            withContext(dispatchers.io) {
                val response = client
                    .formRequest(
                        url = REMOTE_FORM_URL,
                        method = HttpMethod.POST,
                        body = mapOf("entry.1687138646" to errorLog),
                    ).execute()

                val responseString = response.body.string()

                val isSent = response.isSuccessful &&
                    (responseString.contains("form_confirm", true)
                        || responseString.contains("submit another response", true))

                if (!isSent) {
                    val errorMessage = context.getString(R.string.failed_to_send_crash_report)

                    context.showToast(errorMessage)
                }
            }
        }
    }
