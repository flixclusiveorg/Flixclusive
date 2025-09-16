package com.flixclusive.core.common.exception

interface CrashReportSender {
    /**
     *
     * Sends error logs to a remote server (e.g., Google Forms).
     *
     * @param errorLog error log/stack trace/message to be sent.
     * */
    suspend fun send(errorLog: String)
}
