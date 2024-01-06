package com.flixclusive.core.util.log

import android.util.Log

const val FLIXCLUSIVE_LOG_TAG = "FlixclusiveLog"

fun debugLog(message: String) = Log.d(FLIXCLUSIVE_LOG_TAG, message)
fun errorLog(message: String) = Log.e(FLIXCLUSIVE_LOG_TAG, message)