@file:Suppress("ktlint:compose:lambda-param-in-effect")

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
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.core.presentation.mobile.components.material3.dialog.TextAlertDialog
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.splashScreen.component.LoadingTag
import com.flixclusive.feature.splashScreen.screen.consent.ConsentScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.delay
import com.flixclusive.core.strings.R as LocaleR

internal const val APP_TAG_KEY = "tag_image"
internal const val ENTER_DELAY = 800
internal const val EXIT_DELAY = 600

internal val PaddingHorizontal = 8.dp
internal val TagSize = 300.dp

@OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalSharedTransitionApi::class,
)
@Destination
@Composable
internal fun SplashScreen(
    navigator: SplashScreenNavigator,
    viewModel: SplashScreenViewModel = hiltViewModel(),
) {
    val systemPreferences by viewModel.systemPreferences.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userLoggedIn by viewModel.userLoggedIn.collectAsStateWithLifecycle()
    val noUsersFound by viewModel.noUsersFound.collectAsStateWithLifecycle()

    SplashScreenContent(
        systemPreferences = systemPreferences,
        uiState = uiState,
        userLoggedIn = userLoggedIn,
        noUsersFound = noUsersFound,
        updateSettings = viewModel::updateSettings,
        openUpdateScreen = {
            val updateInfo = uiState.newAppUpdateInfo ?: return@SplashScreenContent
            navigator.openUpdateScreen(
                newVersion = updateInfo.versionName,
                updateInfo = updateInfo.changelogs,
                updateUrl = updateInfo.updateUrl,
                isComingFromSplashScreen = true,
            )
        },
        openAddProfileScreen = navigator::openAddProfileScreen,
        openProfilesScreen = navigator::openProfilesScreen,
        openHomeScreen = navigator::openHomeScreen,
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SplashScreenContent(
    systemPreferences: SystemPreferences,
    uiState: SplashScreenUiState,
    userLoggedIn: User?,
    noUsersFound: Boolean,
    updateSettings: (suspend (t: SystemPreferences) -> SystemPreferences) -> Unit,
    openUpdateScreen: () -> Unit,
    openAddProfileScreen: (isInitializing: Boolean) -> Unit,
    openProfilesScreen: (shouldPopBackStack: Boolean) -> Unit,
    openHomeScreen: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PaddingHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SharedTransitionLayout {
            AnimatedContent(
                targetState = systemPreferences.isFirstTimeUserLaunch,
                transitionSpec = {
                    EnterTransition.None togetherWith ExitTransition.None
                },
                label = "splash_screen",
            ) { state ->
                if (state) {
                    ConsentScreen(
                        animatedScope = this@AnimatedContent,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        modifier = Modifier.systemBarsPadding(),
                        onAgree = { isOptingIn ->
                            updateSettings {
                                it.copy(
                                    isFirstTimeUserLaunch = false,
                                    isSendingCrashLogsAutomatically = isOptingIn,
                                )
                            }
                        },
                    )
                } else {
                    var hasErrors by rememberSaveable { mutableStateOf(false) }
                    var isLoading by rememberSaveable { mutableStateOf(false) }
                    var isDoneLoading by rememberSaveable { mutableStateOf(false) }
                    val requiredPermissions = remember { context.getAllRequiredPermissions() }
                    var areAllPermissionsGranted by rememberSaveable { mutableStateOf(requiredPermissions.isEmpty()) }

                    LoadingTag(
                        isLoading = isLoading,
                        animatedScope = this@AnimatedContent,
                        sharedTransitionScope = this@SharedTransitionLayout,
                    )

                    LaunchedEffect(true) {
                        if (!isLoading) {
                            delay(3000L)
                            isLoading = true
                            delay(1000L)
                            isDoneLoading = true
                        }
                    }

                    LaunchedEffect(
                        uiState,
                        areAllPermissionsGranted,
                        isDoneLoading,
                        userLoggedIn,
                    ) {
                        if (areAllPermissionsGranted && isDoneLoading) {
                            val hasAutoUpdate = systemPreferences.isUsingAutoUpdateAppFeature
                            val isAppOutdated = uiState.newAppUpdateInfo != null
                            val updateHasErrors = uiState.error != null
                            val hasOldUserSession = userLoggedIn != null

                            hasErrors = updateHasErrors && hasAutoUpdate

                            if (isAppOutdated && hasAutoUpdate) {
                                openUpdateScreen()
                            } else if (noUsersFound) {
                                openAddProfileScreen(true)
                            } else if (!hasOldUserSession) {
                                openProfilesScreen(true)
                            } else if (!updateHasErrors && hasAutoUpdate) {
                                openHomeScreen()
                            }
                        }
                    }

                    if (hasErrors) {
                        TextAlertDialog(
                            title = stringResource(LocaleR.string.something_went_wrong),
                            message = remember {
                                uiState.error?.stackTraceToString()
                                    ?: context.getString(LocaleR.string.default_error)
                            },
                            confirmButtonLabel = stringResource(LocaleR.string.close_label),
                            dismissButtonLabel = null,
                            onConfirm = openHomeScreen,
                            onDismiss = openHomeScreen,
                        )
                    }

                    if (!areAllPermissionsGranted && isDoneLoading) {
                        PermissionsRequester(
                            permissions = requiredPermissions,
                            onGrantPermissions = { areAllPermissionsGranted = true },
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun SplashScreenBasePreview() {
    FlixclusiveTheme {
        Surface {
            SplashScreenContent(
                systemPreferences = SystemPreferences(isFirstTimeUserLaunch = false),
                uiState = SplashScreenUiState(isLoading = false),
                userLoggedIn = null,
                noUsersFound = true,
                updateSettings = { },
                openUpdateScreen = { },
                openAddProfileScreen = { },
                openProfilesScreen = { },
                openHomeScreen = { },
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
