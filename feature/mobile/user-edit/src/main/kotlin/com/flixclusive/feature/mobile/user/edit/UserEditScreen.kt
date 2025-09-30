package com.flixclusive.feature.mobile.user.edit

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.navigation.navargs.PinVerificationResult
import com.flixclusive.core.navigation.navargs.PinWithHintResult
import com.flixclusive.core.navigation.navigator.PinAction
import com.flixclusive.core.presentation.common.extensions.noIndicationClickable
import com.flixclusive.core.presentation.mobile.components.UserAvatar
import com.flixclusive.core.presentation.mobile.components.UserAvatarDefaults.DefaultAvatarSize
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBar
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.feature.mobile.user.destinations.PinSetupScreenDestination
import com.flixclusive.feature.mobile.user.destinations.PinVerifyScreenDestination
import com.flixclusive.feature.mobile.user.destinations.UserAvatarSelectScreenDestination
import com.flixclusive.feature.mobile.user.edit.tweaks.data.DataTweak
import com.flixclusive.feature.mobile.user.edit.tweaks.identity.IdentityTweak
import com.flixclusive.feature.mobile.user.edit.tweaks.renderTweakUi
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Destination(
    navArgsDelegate = UserEditScreenNavArgs::class
)
@Composable
internal fun UserEditScreen(
    navigator: UserEditScreenNavigator,
    avatarResultRecipient: ResultRecipient<UserAvatarSelectScreenDestination, Int>,
    pinSetupResultRecipient: ResultRecipient<PinSetupScreenDestination, PinWithHintResult>,
    pinRemoveResultRecipient: ResultRecipient<PinVerifyScreenDestination, PinVerificationResult>,
    viewModel: UserEditViewModel = hiltViewModel(),
) {
    val user by viewModel.user.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.onRemoveNavigationState
            .collect {
                when (it) {
                    OnRemoveNavigationState.PopToRoot -> navigator.openProfilesScreen(true)
                    else -> navigator.goBack()
                }
            }
    }

    UserEditScreenContent(
        user = user,
        onEditUser = viewModel::onEditUser,
        onRemoveUser = viewModel::onRemoveUser,
        onClearSearchHistory = viewModel::onClearSearchHistory,
        onClearLibraries = viewModel::onClearLibraries,
        openUserAvatarSelectScreen = navigator::openUserAvatarSelectScreen,
        goBack = navigator::goBack,
        openUserPinScreen = navigator::openUserPinScreen,
        avatarResultRecipient = avatarResultRecipient,
        pinSetupResultRecipient = pinSetupResultRecipient,
        pinRemoveResultRecipient = pinRemoveResultRecipient,
    )
}

@Composable
private fun UserEditScreenContent(
    user: User,
    onEditUser: (User) -> Unit,
    onRemoveUser: (userId: Int) -> Unit,
    onClearSearchHistory: (userId: Int) -> Unit,
    onClearLibraries: (userId: Int, libraries: List<Library>) -> Unit,
    openUserAvatarSelectScreen: (selected: Int) -> Unit,
    goBack: () -> Unit,
    openUserPinScreen: (action: PinAction) -> Unit,
    avatarResultRecipient: ResultRecipient<UserAvatarSelectScreenDestination, Int>,
    pinSetupResultRecipient: ResultRecipient<PinSetupScreenDestination, PinWithHintResult>,
    pinRemoveResultRecipient: ResultRecipient<PinVerifyScreenDestination, PinVerificationResult>,
) {
    val tweaks =
        remember(user.pin) {
            listOf(
                IdentityTweak(
                    initialName = user.name,
                    userHasPin = user.pin != null,
                    onOpenPinScreen = { isRemovingPin ->
                        val action = if (isRemovingPin) {
                            PinAction.Verify(user.pin!!)
                        } else {
                            PinAction.Setup
                        }

                        openUserPinScreen(action)
                    },
                    onNameChange = {
                        onEditUser(user.copy(name = it))
                    },
                ),
                DataTweak(
                    onClearSearchHistory = { onClearSearchHistory(user.id) },
                    onDeleteProfile = { onRemoveUser(user.id) },
                    onClearLibraries = { selection ->
                        onClearLibraries(user.id, selection)
                    },
                ),
            )
        }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    avatarResultRecipient.onNavResult { result ->
        if (result is NavResult.Value) {
            onEditUser(user.copy(image = result.value))
        }
    }

    pinSetupResultRecipient.onNavResult { result ->
        if (result is NavResult.Value) {
            val (pin, hint) = result.value
            onEditUser(user.copy(pin = pin, pinHint = hint))
        }
    }

    pinRemoveResultRecipient.onNavResult { result ->
        if (result is NavResult.Value && result.value.isVerified) {
            onEditUser(user.copy(pin = null, pinHint = null))
        }
    }

    Scaffold(
        topBar = {
            CommonTopBar(
                title = stringResource(LocaleR.string.edit_profile),
                onNavigate = { goBack() },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = PaddingValues(getAdaptiveDp(10.dp)),
                modifier = Modifier
                    .fillMaxSize()
                    .noIndicationClickable {
                        keyboardController?.hide()
                        focusManager.clearFocus(true)
                    },
            ) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(getAdaptiveDp(5.dp)),
                        ) {
                            UserAvatar(
                                avatar = user.image,
                                borderWidth = 0.dp,
                                shadowBlur = 0.dp,
                                shadowSpread = 0.dp,
                                modifier = Modifier
                                    .height(
                                        getAdaptiveDp(
                                            dp = (DefaultAvatarSize.value * 1.2).dp,
                                            increaseBy = 80.dp,
                                        ),
                                    )
                                    .aspectRatio(1F),
                            )
                        }

                        ChangeImageButton(
                            onClick = { openUserAvatarSelectScreen(user.image) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd),
                        )
                    }
                }

                tweaks.forEach {
                    renderTweakUi(it)
                }
            }
        }
    }
}

@Composable
private fun ChangeImageButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonSize = getAdaptiveDp(30.dp, 10.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(buttonSize)
                .background(
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                )
                .clickable(
                    indication =
                        ripple(
                            bounded = false,
                            radius = buttonSize / 2,
                        ),
                    interactionSource = null,
                ) {
                    onClick()
                },
    ) {
        Icon(
            painter = painterResource(UiCommonR.drawable.edit),
            contentDescription = stringResource(LocaleR.string.change_avatar_content_desc),
            tint = MaterialTheme.colorScheme.surface,
            modifier =
                Modifier.size(
                    getAdaptiveDp(
                        dp = 18.dp,
                        increaseBy = 10.dp,
                    ),
                ),
        )
    }
}

@Suppress("OVERRIDE_DEPRECATION")
@SuppressLint("ComposableNaming")
@Preview
@Composable
private fun UserEditScreenBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier =
                Modifier
                    .fillMaxSize(),
        ) {
            UserEditScreenContent(
                user = User(
                    id = 1,
                    name = "John Doe",
                    image = 1,
                    pin = "1234",
                    pinHint = "My PIN",
                ),
                onEditUser = {},
                onRemoveUser = {},
                onClearSearchHistory = {},
                onClearLibraries = { _, _ -> },
                openUserAvatarSelectScreen = {},
                goBack = {},
                openUserPinScreen = {},
                avatarResultRecipient =
                    object : ResultRecipient<UserAvatarSelectScreenDestination, Int> {
                        @Composable
                        override fun onNavResult(listener: (NavResult<Int>) -> Unit) = Unit

                        @Composable
                        override fun onResult(listener: (Int) -> Unit) = Unit
                    },
                pinSetupResultRecipient =
                    object : ResultRecipient<PinSetupScreenDestination, PinWithHintResult> {
                        @Composable
                        override fun onNavResult(listener: (NavResult<PinWithHintResult>) -> Unit) = Unit

                        @Composable
                        override fun onResult(listener: (PinWithHintResult) -> Unit) = Unit
                    },
                pinRemoveResultRecipient =
                    object : ResultRecipient<PinVerifyScreenDestination, PinVerificationResult> {
                        @Composable
                        override fun onNavResult(listener: (NavResult<PinVerificationResult>) -> Unit) = Unit

                        @Composable
                        override fun onResult(listener: (PinVerificationResult) -> Unit) = Unit
                    },
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun UserEditScreenCompactLandscapePreview() {
    UserEditScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun UserEditScreenMediumPortraitPreview() {
    UserEditScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun UserEditScreenMediumLandscapePreview() {
    UserEditScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun UserEditScreenExtendedPortraitPreview() {
    UserEditScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun UserEditScreenExtendedLandscapePreview() {
    UserEditScreenBasePreview()
}
