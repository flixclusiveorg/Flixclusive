@file:Suppress("ktlint:compose:lambda-param-in-effect")

package com.flixclusive.mobile.component

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.flixclusive.BuildConfig
import com.flixclusive.feature.mobile.markdown.destinations.MarkdownScreenDestination
import com.flixclusive.navigation.extensions.isSplashScreen
import com.flixclusive.navigation.extensions.navigateIfResumed
import com.ramcosta.composedestinations.spec.Route
import kotlinx.coroutines.delay

@SuppressLint("DiscouragedApi")
@Composable
internal fun DisplayChangelogsObserver(
    navController: NavController,
    hasNotSeenNewChangelogs: Boolean,
    currentSelectedScreen: Route,
    onSaveLastSeenChangelogs: (version: Long) -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(hasNotSeenNewChangelogs, currentSelectedScreen) {
        if (!hasNotSeenNewChangelogs || currentSelectedScreen.isSplashScreen) {
            return@LaunchedEffect
        }

        delay(1000L) // Add delay for smooth transition

        onSaveLastSeenChangelogs(BuildConfig.VERSION_CODE.toLong())

        val changelogsId = context.resources.getIdentifier(
            "changelog_${BuildConfig.VERSION_NAME}",
            "array",
            context.packageName,
        )

        if (changelogsId == 0) {
            return@LaunchedEffect
        }

        val (title, changelogs) = context.resources.getStringArray(changelogsId)

        navController.navigateIfResumed(
            direction = MarkdownScreenDestination(title = title, description = changelogs),
        ) {
            launchSingleTop = true
            restoreState = true
        }
    }
}
