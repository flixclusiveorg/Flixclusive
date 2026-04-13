@file:Suppress("ktlint:compose:lambda-param-in-effect")

package com.flixclusive.feature.mobile.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
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
import androidx.core.content.ContextCompat
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
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import com.flixclusive.core.strings.R as LocaleR


@Destination<ExternalModuleGraph>
@Composable
internal fun OnboardingScreen(
    navigator: OnboardingScreenNavigator,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val systemPreferences by viewModel.systemPreferences.collectAsStateWithLifecycle()

    var notificationsGranted by remember { mutableStateOf(isNotificationsGranted(context)) }
    var unknownSourcesAllowed by remember { mutableStateOf(isUnknownSourcesAllowed(context)) }

    val grantedPermissions = remember(context) {
        getPreGrantedPermissions(
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

    val unknownSourcesSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        unknownSourcesAllowed = isUnknownSourcesAllowed(context)
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
                notificationsGranted = isNotificationsGranted(context)
                unknownSourcesAllowed = isUnknownSourcesAllowed(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    OnboardingScreenContent(
        notificationsGranted = notificationsGranted,
        unknownSourcesAllowed = unknownSourcesAllowed,
        storageDirectoryUri = systemPreferences.storageDirectoryUri,
        grantedPermissions = grantedPermissions,
        requestNotificationsPermission = {
            if (Build.VERSION.SDK_INT >= 33) {
                notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        openUnknownSourcesSettings = {
            unknownSourcesSettingsLauncher.launch(createUnknownSourcesIntent(context.packageName))
        },
        openStorageDirectoryPicker = { storageDirectoryPicker.launch(null) },
        finishOnboarding = {
            viewModel.completeOnboarding()
            navigator.openAddProfileScreen(true)
        },
    )
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
private fun OnboardingScreenContent(
    notificationsGranted: Boolean,
    unknownSourcesAllowed: Boolean,
    storageDirectoryUri: String?,
    grantedPermissions: List<GrantedPermissionItem>,
    requestNotificationsPermission: () -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
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
                    (fadeIn(tweenAlpha) +
                        slideInHorizontally(animationSpec = tweenOffset) { it / widthDivisor }) togetherWith
                        (fadeOut(tweenAlpha) +
                            slideOutHorizontally(animationSpec = tweenOffset) { -it / widthDivisor })
                } else {
                    (fadeIn(tweenAlpha) +
                        slideInHorizontally(animationSpec = tweenOffset) { -it / widthDivisor }) togetherWith
                        (fadeOut(tweenAlpha) +
                            slideOutHorizontally(animationSpec = tweenOffset) { it / widthDivisor })
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
                    grantedPermissions = grantedPermissions,
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

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
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
                            onClick = { currentStep-- },
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(text = stringResource(LocaleR.string.back))
                        }
                    }

                    Button(
                        enabled = isContinueEnabled,
                        onClick = {
                            when {
                                OnboardingStep.entries[currentStep] == OnboardingStep.FinishUp -> finishOnboarding()
                                currentStep < OnboardingStep.entries.lastIndex -> currentStep++
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

private fun isNotificationsGranted(context: Context): Boolean {
    return when {
        Build.VERSION.SDK_INT < 33 -> true
        else -> ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

@Suppress("DEPRECATION")
private fun isUnknownSourcesAllowed(context: Context): Boolean {
    return when {
        Build.VERSION.SDK_INT >= 26 -> context.packageManager.canRequestPackageInstalls()
        else -> {
            runCatching {
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.INSTALL_NON_MARKET_APPS,
                    0,
                ) == 1
            }.getOrDefault(true)
        }
    }
}

private fun getPreGrantedPermissions(
    context: Context,
    excludedPermissions: Set<String>,
): List<GrantedPermissionItem> {
    val pm = context.packageManager

    val requestedPermissions = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
            ).requestedPermissions
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions
        }
    }.getOrNull().orEmpty().toList()

    return requestedPermissions
        .asSequence()
        .filterNot { it in excludedPermissions }
        .filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        .distinct()
        .map { permission ->
            val label = runCatching {
                pm.getPermissionInfo(permission, 0)
                    .loadLabel(pm)
                    .toString()
            }.getOrNull().orEmpty()

            GrantedPermissionItem(
                label = label.ifBlank { permission.substringAfterLast('.') },
                name = permission,
            )
        }
        .sortedBy { it.label.lowercase() }
        .toList()
}

private fun createUnknownSourcesIntent(packageName: String): Intent {
    return when {
        Build.VERSION.SDK_INT >= 26 -> Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:$packageName".toUri(),
        )

        else -> Intent(Settings.ACTION_SECURITY_SETTINGS)
    }
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
                storageDirectoryUri = null,
                grantedPermissions = listOf(
                    GrantedPermissionItem(
                        label = "Full network access",
                        name = "android.permission.INTERNET",
                    ),
                    GrantedPermissionItem(
                        label = "View network connections",
                        name = "android.permission.ACCESS_NETWORK_STATE",
                    ),
                ),
                requestNotificationsPermission = {},
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
                storageDirectoryUri = null,
                grantedPermissions = listOf(
                    GrantedPermissionItem(
                        label = "Full network access",
                        name = "android.permission.INTERNET",
                    ),
                ),
                requestNotificationsPermission = {},
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
                storageDirectoryUri = null,
                grantedPermissions = emptyList(),
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
                storageDirectoryUri = "content://com.android.externalstorage.documents/tree/primary%3AFlixclusive",
                grantedPermissions = emptyList(),
                requestNotificationsPermission = {},
                openUnknownSourcesSettings = {},
                openStorageDirectoryPicker = {},
                finishOnboarding = {},
                initialStepIndex = OnboardingStep.FinishUp.ordinal,
            )
        }
    }
}
