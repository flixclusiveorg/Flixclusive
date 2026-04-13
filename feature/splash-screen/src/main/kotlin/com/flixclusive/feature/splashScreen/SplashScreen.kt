package com.flixclusive.feature.splashScreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.core.presentation.mobile.components.material3.dialog.TextAlertDialog
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.splashScreen.component.LoadingTag
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import com.flixclusive.core.strings.R as LocaleR

internal const val APP_TAG_KEY = "tag_image"
internal const val ENTER_DELAY = 800
internal const val EXIT_DELAY = 600

internal val PaddingHorizontal = 8.dp
internal val TagSize = 300.dp

@OptIn(
    ExperimentalSharedTransitionApi::class,
)
@Destination<ExternalModuleGraph>
@Composable
internal fun SplashScreen(
    navigator: SplashScreenNavigator,
    viewModel: SplashScreenViewModel = hiltViewModel(),
) {
    val systemPreferences by viewModel.systemPreferences.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userLoggedIn by viewModel.userLoggedIn.collectAsStateWithLifecycle()
    val noUsersFound by viewModel.noUsersFound.collectAsStateWithLifecycle()

    val preferences = systemPreferences ?: return
    val hasAppUpdateErrors = uiState.appUpdateError != null && preferences.isUsingAutoUpdateAppFeature
    var hasNavigated by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(
        preferences.isFirstTimeUserLaunch,
        uiState.isLoading,
        hasAppUpdateErrors,
        uiState.newAppUpdateInfo,
        preferences.isUsingAutoUpdateAppFeature,
        userLoggedIn,
        noUsersFound,
    ) {
        if (hasNavigated) return@LaunchedEffect

        if (preferences.isFirstTimeUserLaunch) {
            hasNavigated = true
            navigator.openOnboardingScreen()
            return@LaunchedEffect
        }

        if (!uiState.isLoading && !hasAppUpdateErrors) {
            val hasAutoUpdate = preferences.isUsingAutoUpdateAppFeature
            val updateInfo = uiState.newAppUpdateInfo
            val hasOldUserSession = userLoggedIn != null

            hasNavigated = true
            if (updateInfo != null && hasAutoUpdate) {
                navigator.openUpdateScreen(
                    newVersion = updateInfo.versionName,
                    updateInfo = updateInfo.changelogs,
                    updateUrl = updateInfo.updateUrl,
                    isComingFromSplashScreen = true,
                )
            } else if (noUsersFound) {
                navigator.openAddProfileScreen(true)
            } else if (!hasOldUserSession) {
                navigator.openProfilesScreen(true)
            } else {
                navigator.openHomeScreen()
            }
        }
    }

    SplashScreenContent(
        isLoading = uiState.isLoading,
        showAppUpdateErrorDialog = hasAppUpdateErrors && !uiState.isLoading,
        appUpdateErrorMessage = uiState.appUpdateError?.uiText?.asString() ?: "",
        onConsumeAppUpdateError = viewModel::onConsumeAppUpdateError,
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SplashScreenContent(
    isLoading: Boolean,
    showAppUpdateErrorDialog: Boolean,
    appUpdateErrorMessage: String,
    onConsumeAppUpdateError: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PaddingHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SharedTransitionLayout {
            AnimatedContent(
                targetState = isLoading,
                transitionSpec = {
                    EnterTransition.None togetherWith ExitTransition.None
                },
                label = "splash_screen",
            ) { state ->
                LoadingTag(
                    isLoading = state,
                    animatedScope = this@AnimatedContent,
                    sharedTransitionScope = this@SharedTransitionLayout,
                )
            }
        }

        if (showAppUpdateErrorDialog) {
            TextAlertDialog(
                title = stringResource(LocaleR.string.something_went_wrong),
                message = appUpdateErrorMessage,
                confirmButtonLabel = stringResource(LocaleR.string.close),
                dismissButtonLabel = null,
                onConfirm = onConsumeAppUpdateError,
                onDismiss = onConsumeAppUpdateError,
            )
        }
    }
}

@Preview
@Composable
private fun SplashScreenBasePreview() {
    FlixclusiveTheme {
        Surface {
            SplashScreenContent(
                isLoading = false,
                showAppUpdateErrorDialog = false,
                appUpdateErrorMessage = "",
                onConsumeAppUpdateError = { },
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun SplashScreenCompactLandscapePreview() {
    SplashScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun SplashScreenMediumPortraitPreview() {
    SplashScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun SplashScreenMediumLandscapePreview() {
    SplashScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun SplashScreenExtendedPortraitPreview() {
    SplashScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun SplashScreenExtendedLandscapePreview() {
    SplashScreenBasePreview()
}
