package com.flixclusive.core.ui.tv.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.ramcosta.composedestinations.spec.Route

private val LocalLastFocusedItemPerDestination = compositionLocalOf<MutableMap<String, String>?> { null }
private val LocalFocusTransferredOnLaunch = compositionLocalOf<MutableState<Boolean>?> { null }
private val LocalCurrentRoute = compositionLocalOf<String?> { null }

@Composable
fun LocalLastFocusedItemPerDestinationProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLastFocusedItemPerDestination provides remember { mutableMapOf() }, content = content)
}

@Composable
fun LocalFocusTransferredOnLaunchProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalFocusTransferredOnLaunch provides remember { mutableStateOf(false) }, content = content)
}

@Composable
fun LocalCurrentRouteProvider(currentSelectedScreen: Route, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCurrentRoute provides currentSelectedScreen.route, content = content)
}

@Composable
fun useLocalLastFocusedItemPerDestination(): MutableMap<String, String> {
    return LocalLastFocusedItemPerDestination.current ?: throw RuntimeException("Please wrap your app with LocalLastFocusedItemPerDestinationProvider")
}

@Composable
fun useLocalCurrentRoute(): String {
    return LocalCurrentRoute.current ?: throw RuntimeException("Please wrap your app with LocalCurrentRoute")
}

@Composable
fun useLocalFocusTransferredOnLaunch(): MutableState<Boolean> {
    return LocalFocusTransferredOnLaunch.current ?: throw RuntimeException("Please wrap your app with LocalLastFocusedItemPerDestinationProvider")
}