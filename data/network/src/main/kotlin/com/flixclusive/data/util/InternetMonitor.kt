package com.flixclusive.data.util

import kotlinx.coroutines.flow.Flow

interface InternetMonitor {
    val isOnline: Flow<Boolean>
}