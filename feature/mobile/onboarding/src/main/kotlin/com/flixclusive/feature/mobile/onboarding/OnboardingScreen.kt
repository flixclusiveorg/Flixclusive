@file:Suppress("ktlint:compose:lambda-param-in-effect")

package com.flixclusive.feature.mobile.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.feature.mobile.onboarding.component.FinishUpStep
import com.flixclusive.feature.mobile.onboarding.component.GrantedPermissionItem
import com.flixclusive.feature.mobile.onboarding.component.OnboardingStepIndicator
import com.flixclusive.feature.mobile.onboarding.component.PermissionsStep
import com.flixclusive.feature.mobile.onboarding.component.StorageStep
import com.flixclusive.feature.mobile.onboarding.component.WelcomeStep
import com.hippo.unifile.UniFile
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import com.flixclusive.core.strings.R as LocaleR

@Destination<ExternalModuleGraph>
@Composable
internal fun OnboardingScreen(
    navigator: NavigatorOnboardingScreen,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val systemPreferences by viewModel.systemPreferences.collectAsStateWithLifecycle()
    val filePath = remember(systemPreferences.storageDirectoryUri) {
        systemPreferences.storageDirectoryUri?.toUri()?.let { uri ->
            UniFile.fromUri(context, uri).filePath
        }
    }

    var notificationsGranted by remember { mutableStateOf(PermissionUtil.isNotificationsGranted(context)) }
    var storageAccessGranted by remember { mutableStateOf(PermissionUtil.isStorageAccessGranted(context)) }
    var unknownSourcesAllowed by remember { mutableStateOf(PermissionUtil.isUnknownSourcesAllowed(context)) }

    val grantedPermissions = remember(context) {
        PermissionUtil.getPreGrantedPermissions(
            context = context,
            excludedPermissions = mutableSetOf(
                Manifest.permission.REQUEST_INSTALL_PACKAGES,
            ).also {
                if (Build.VERSION.SDK_INT >= 33) {
                    it.add(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )
    }

    val notificationsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsGranted = granted
    }

    val manageStorageSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        storageAccessGranted = PermissionUtil.isStorageAccessGranted(context)
    }

    val storageAccessPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        val isWritingGranted = granted[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
        val isReadingGranted = granted[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        storageAccessGranted = isWritingGranted && isReadingGranted
    }

    val unknownSourcesSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        unknownSourcesAllowed = PermissionUtil.isUnknownSourcesAllowed(context)
    }

    val storageDirectoryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }

        viewModel.updateStorageDirectoryUri(uri.toString())
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsGranted = PermissionUtil.isNotificationsGranted(context)
                unknownSourcesAllowed = PermissionUtil.isUnknownSourcesAllowed(context)
                storageAccessGranted = PermissionUtil.isStorageAccessGranted(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.nextStepNavigation.collect { navigation ->
            when (navigation) {
                NextStepNavigation.CONTINUE_ONBOARDING -> navigator.navigateToAddProfileScreen(true)
                NextStepNavigation.HOME -> navigator.navigateToHomeScreen()
            }
        }
    }

    OnboardingScreenContent(
        notificationsGranted = notificationsGranted,
        storageAccessGranted = storageAccessGranted,
        unknownSourcesAllowed = unknownSourcesAllowed,
        storageDirectoryUri = filePath,
        grantedPermissions = grantedPermissions,
        finishOnboarding = viewModel::completeOnboarding,
        openUnknownSourcesSettings = {
            unknownSourcesSettingsLauncher.launch(PermissionUtil.createUnknownSourcesIntent(context.packageName))
        },
        openStorageDirectoryPicker = { storageDirectoryPicker.launch(null) },
        requestNotificationsPermission = {
            if (Build.VERSION.SDK_INT >= 33) {
                notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        requestStorageAccess = {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    manageStorageSettingsLauncher.launch(PermissionUtil.createManageStorageIntent(context))
                }

                else -> {
                    storageAccessPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                        ),
                    )
                }
            }
        },
    )
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
private fun OnboardingScreenContent(
    notificationsGranted: Boolean,
    storageAccessGranted: Boolean,
    unknownSourcesAllowed: Boolean,
    storageDirectoryUri: String?,
    grantedPermissions: List<GrantedPermissionItem>,
    requestNotificationsPermission: () -> Unit,
    requestStorageAccess: () -> Unit,
    openUnknownSourcesSettings: () -> Unit,
    openStorageDirectoryPicker: () -> Unit,
    finishOnboarding: () -> Unit,
    initialStepIndex: Int = 0,
) {
    val initialStep = initialStepIndex.coerceIn(0, OnboardingStep.entries.lastIndex)
    var currentStep by rememberSaveable(initialStep) { mutableIntStateOf(initialStep) }

    val isStorageSelected = storageDirectoryUri != null

    val isContinueEnabled = when (OnboardingStep.entries[currentStep]) {
        OnboardingStep.Welcome -> true
        OnboardingStep.Permissions -> unknownSourcesAllowed
        OnboardingStep.Storage -> isStorageSelected
        OnboardingStep.FinishUp -> true
    }

    val primaryButtonLabel = when (OnboardingStep.entries[currentStep]) {
        OnboardingStep.FinishUp -> stringResource(LocaleR.string.finish)
        else -> stringResource(LocaleR.string.next)
    }

    Box(
        modifier = Modifier
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter),
        ) {
            OnboardingStepIndicator(
                currentIndex = currentStep.coerceIn(0, 3),
                steps = OnboardingStep.entries.size,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 14.dp),
            )

            AnimatedContent(
                targetState = currentStep,
                label = "onboarding_step",
                transitionSpec = {
                    val tweenOffset = tween<IntOffset>(durationMillis = 300)
                    val tweenAlpha = tween<Float>(durationMillis = 300)
                    val widthDivisor = 6

                    if (targetState > initialState) {
                        (
                            fadeIn(tweenAlpha) +
                                slideInHorizontally(animationSpec = tweenOffset) { it / widthDivisor }
                        ) togetherWith
                            (
                                fadeOut(tweenAlpha) +
                                    slideOutHorizontally(animationSpec = tweenOffset) { -it / widthDivisor }
                            )
                    } else {
                        (
                            fadeIn(tweenAlpha) +
                                slideInHorizontally(animationSpec = tweenOffset) { -it / widthDivisor }
                        ) togetherWith
                            (
                                fadeOut(tweenAlpha) +
                                    slideOutHorizontally(animationSpec = tweenOffset) { it / widthDivisor }
                            )
                    }.using(
                        SizeTransform(
                            clip = false,
                            sizeAnimationSpec = { _, _ -> tween(durationMillis = 300) },
                        ),
                    )
                },
                contentAlignment = Alignment.TopStart,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { stepIndex ->
                when (OnboardingStep.entries[stepIndex]) {
                    OnboardingStep.Welcome -> WelcomeStep()

                    OnboardingStep.Permissions -> PermissionsStep(
                        notificationsGranted = notificationsGranted,
                        unknownSourcesAllowed = unknownSourcesAllowed,
                        storageAccessGranted = storageAccessGranted,
                        grantedPermissions = grantedPermissions,
                        requestStorageAccess = requestStorageAccess,
                        requestNotificationsPermission = requestNotificationsPermission,
                        openUnknownSourcesSettings = openUnknownSourcesSettings,
                    )

                    OnboardingStep.Storage -> StorageStep(
                        storageDirectoryUri = storageDirectoryUri,
                        onPickStorageDirectory = openStorageDirectoryPicker,
                    )

                    OnboardingStep.FinishUp -> FinishUpStep()
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        ) {
            if (!isContinueEnabled) {
                Text(
                    text = when (OnboardingStep.entries[currentStep]) {
                        OnboardingStep.Permissions -> stringResource(R.string.onboarding_permissions_required_hint)
                        OnboardingStep.Storage -> stringResource(R.string.onboarding_storage_required_hint)
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle(),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val showBackButton = currentStep > 0

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AnimatedVisibility(
                        visible = showBackButton,
                        modifier = Modifier.weight(1f),
                        enter = fadeIn(tween(200)) + slideInHorizontally(tween(300)) { -it / 6 },
                        exit = fadeOut(tween(200)) + slideOutHorizontally(tween(300)) { -it / 6 },
                    ) {
                        TextButton(
                            onClick = {
                                currentStep = (currentStep - 1).coerceAtLeast(0)
                            },
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(text = stringResource(LocaleR.string.back))
                        }
                    }

                    Button(
                        enabled = isContinueEnabled,
                        onClick = {
                            when {
                                OnboardingStep.entries[currentStep] == OnboardingStep.FinishUp -> {
                                    finishOnboarding()
                                }

                                currentStep < OnboardingStep.entries.lastIndex -> {
                                    currentStep =
                                        (currentStep + 1).coerceAtMost(
                                            OnboardingStep.entries.lastIndex
                                        )
                                }
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .animateContentSize()
                            .weight(1f),
                    ) {
                        Text(text = primaryButtonLabel)
                    }
                }
            }
        }
    }
}

private enum class OnboardingStep {
    Welcome,
    Permissions,
    Storage,
    FinishUp,
}

@Preview
@Composable
private fun OnboardingScreenBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            OnboardingScreenContent(
                notificationsGranted = false,
                unknownSourcesAllowed = false,
                storageAccessGranted = false,
                storageDirectoryUri = null,
                grantedPermissions = listOf(
                    GrantedPermissionItem(
                        label = "Full network access",
                        description = "android.permission.INTERNET",
                    ),
                    GrantedPermissionItem(
                        label = "View network connections",
                        description = "android.permission.ACCESS_NETWORK_STATE",
                    ),
                ),
                requestNotificationsPermission = {},
                requestStorageAccess = {},
                openUnknownSourcesSettings = {},
                openStorageDirectoryPicker = {},
                finishOnboarding = {},
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun OnboardingScreenCompactLandscapePreview() {
    OnboardingScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun OnboardingScreenMediumPortraitPreview() {
    OnboardingScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun OnboardingScreenMediumLandscapePreview() {
    OnboardingScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun OnboardingScreenExtendedPortraitPreview() {
    OnboardingScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun OnboardingScreenExtendedLandscapePreview() {
    OnboardingScreenBasePreview()
}

@Preview
@Composable
private fun OnboardingScreenPermissionsStepPreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            OnboardingScreenContent(
                notificationsGranted = false,
                unknownSourcesAllowed = false,
                storageAccessGranted = false,
                storageDirectoryUri = null,
                grantedPermissions = listOf(
                    GrantedPermissionItem(
                        label = "Full network access",
                        description = "android.permission.INTERNET",
                    ),
                ),
                requestNotificationsPermission = {},
                requestStorageAccess = {},
                openUnknownSourcesSettings = {},
                openStorageDirectoryPicker = {},
                finishOnboarding = {},
                initialStepIndex = OnboardingStep.Permissions.ordinal,
            )
        }
    }
}

@Preview
@Composable
private fun OnboardingScreenStorageStepPreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            OnboardingScreenContent(
                notificationsGranted = true,
                unknownSourcesAllowed = true,
                storageAccessGranted = true,
                storageDirectoryUri = null,
                grantedPermissions = emptyList(),
                requestStorageAccess = {},
                requestNotificationsPermission = {},
                openUnknownSourcesSettings = {},
                openStorageDirectoryPicker = {},
                finishOnboarding = {},
                initialStepIndex = OnboardingStep.Storage.ordinal,
            )
        }
    }
}

@Preview
@Composable
private fun OnboardingScreenNextStepsStepPreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            OnboardingScreenContent(
                notificationsGranted = true,
                unknownSourcesAllowed = true,
                storageAccessGranted = true,
                storageDirectoryUri = "content://com.android.externalstorage.documents/tree/primary%3AFlixclusive",
                grantedPermissions = emptyList(),
                requestStorageAccess = {},
                requestNotificationsPermission = {},
                openUnknownSourcesSettings = {},
                openStorageDirectoryPicker = {},
                finishOnboarding = {},
                initialStepIndex = OnboardingStep.FinishUp.ordinal,
            )
        }
    }
}
