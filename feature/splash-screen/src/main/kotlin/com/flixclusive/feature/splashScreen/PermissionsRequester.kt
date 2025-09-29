package com.flixclusive.feature.splashScreen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.presentation.mobile.components.material3.dialog.TextAlertDialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.flixclusive.core.strings.R as LocaleR

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun PermissionsRequester(
    onGrantPermissions: (Boolean) -> Unit,
    permissions: List<String>,
) {
    val permissionsState =
        rememberMultiplePermissionsState(permissions)

    val textToShow =
        if (permissionsState.shouldShowRationale) {
            stringResource(LocaleR.string.permissions_persist_request_message)
        } else {
            stringResource(LocaleR.string.permissions_request_message)
        }

    if (!permissionsState.allPermissionsGranted) {
        TextAlertDialog(
            title = stringResource(LocaleR.string.splash_notice_permissions_header),
            message = textToShow,
            confirmButtonLabel = stringResource(LocaleR.string.allow),
            dismissButtonLabel = null,
            onConfirm = permissionsState::launchMultiplePermissionRequest,
            onDismiss = permissionsState::launchMultiplePermissionRequest,
        )
    }

    if (permissionsState.allPermissionsGranted) {
        onGrantPermissions(true)
    }
}

internal fun Context.getAllRequiredPermissions(): List<String> {
    val requiredPermissions = mutableListOf<String>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }

//    TODO: Uncomment when needed
    val isAndroid6AndAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    val isAndroid9AndBelow = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
    if (isAndroid6AndAbove && isAndroid9AndBelow) {
        requiredPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val isAndroid10AndBelow = Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
    if (isAndroid6AndAbove && isAndroid10AndBelow) {
        requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    val isAndroid11AndAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    if (isAndroid11AndAbove) {
        requiredPermissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
    }

    return requiredPermissions.filterNot { permission ->
        checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}
