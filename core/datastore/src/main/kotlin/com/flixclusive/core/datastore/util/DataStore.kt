package com.flixclusive.core.datastore.util

import androidx.datastore.core.DataStore
import com.flixclusive.core.datastore.model.FlixclusivePrefs
import com.flixclusive.core.util.coroutines.AppDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking

fun <T : FlixclusivePrefs> DataStore<T>.awaitFirst() =
    runBlocking(context = AppDispatchers.IO.dispatcher) { data.first() }

fun <T : FlixclusivePrefs> Flow<T>.awaitFirst() = runBlocking(context = AppDispatchers.IO.dispatcher) { first() }

fun <T : FlixclusivePrefs> DataStore<T>.asStateFlow(
    scope: CoroutineScope,
    started: SharingStarted = SharingStarted.WhileSubscribed(5000),
    initialValue: T = awaitFirst(),
) = data.asStateFlow(
    scope = scope,
    started = started,
    initialValue = initialValue,
)

fun <T : FlixclusivePrefs> Flow<T>.asStateFlow(
    scope: CoroutineScope,
    started: SharingStarted = SharingStarted.WhileSubscribed(5000),
    initialValue: T = awaitFirst(),
) = stateIn(
    scope = scope,
    started = started,
    initialValue = initialValue,
)
