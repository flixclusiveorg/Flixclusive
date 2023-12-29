package com.flixclusive.utils

import android.util.Log
import com.flixclusive.BuildConfig
import com.flixclusive.common.Constants.FLIXCLUSIVE_LOG_TAG

object LoggerUtils {
    fun debugLog(message: String) {
        if(BuildConfig.DEBUG) {
            Log.d(FLIXCLUSIVE_LOG_TAG, message)
        }
    }
    fun errorLog(message: String) {
        if(BuildConfig.DEBUG) {
            Log.e(FLIXCLUSIVE_LOG_TAG, message)
        }
    }
}