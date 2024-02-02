package com.flixclusive.core.ui.tv.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import com.ramcosta.composedestinations.spec.Route

private val LocalLastFocusedItemPerDestination = compositionLocalOf<MutableMap<String, String>> {
    error("Please wrap your app with LocalLastFocusedItemPerDestinationProvider")
}
private val LocalFocusTransferredOnLaunch = compositionLocalOf<MutableState<Boolean>> {
    error("Please wrap your app with LocalFocusTransferredOnLaunch")
}
private val LocalCurrentRoute = compositionLocalOf<String> {
    error("Please wrap your app with LocalCurrentRoute")
}
private val LocalLastFocusedItemFocusedRequester = compositionLocalOf<MutableState<FocusRequester>> {
    error("Please wrap your app with LocalLastFocusedItemFocusedRequester")
}

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
fun LocalLastFocusedItemFocusedRequesterProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLastFocusedItemFocusedRequester provides remember { mutableStateOf(FocusRequester()) }, content = content)
}

@Composable
fun useLocalLastFocusedItemFocusedRequester()
    = LocalLastFocusedItemFocusedRequester.current

@Composable
fun useLocalLastFocusedItemPerDestination()
    = LocalLastFocusedItemPerDestination.current

@Composable
fun useLocalCurrentRoute()
    = LocalCurrentRoute.current

@Composable
fun useLocalFocusTransferredOnLaunch()
    = LocalFocusTransferredOnLaunch.current