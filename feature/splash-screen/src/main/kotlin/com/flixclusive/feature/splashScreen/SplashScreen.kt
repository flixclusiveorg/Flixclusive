package com.flixclusive.feature.splashScreen

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.presentation.mobile.components.material3.dialog.TextAlertDialog
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.splashScreen.component.LoadingTag
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import com.flixclusive.core.strings.R as LocaleR


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

    val preferences = systemPreferences ?: return
    val hasAppUpdateErrors = uiState.appUpdateError != null && preferences.isUsingAutoUpdateAppFeature

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                SplashNavigationEvent.Onboarding -> navigator.openOnboardingScreen()
                is SplashNavigationEvent.AppUpdate -> navigator.openUpdateScreen(
                    newVersion = event.info.versionName,
                    updateInfo = event.info.changelogs,
                    updateUrl = event.info.updateUrl,
                    isComingFromSplashScreen = true,
                )
                SplashNavigationEvent.AddProfile -> navigator.openAddProfileScreen(true)
                SplashNavigationEvent.ChooseProfile -> navigator.openProfilesScreen(true)
                SplashNavigationEvent.Home -> navigator.openHomeScreen()
            }
        }
    }

    SplashScreenContent(
        showAppUpdateErrorDialog = hasAppUpdateErrors,
        appUpdateErrorMessage = uiState.appUpdateError?.uiText?.asString() ?: "",
        onConsumeAppUpdateError = viewModel::onConsumeAppUpdateError,
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SplashScreenContent(
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
        LoadingTag()

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
