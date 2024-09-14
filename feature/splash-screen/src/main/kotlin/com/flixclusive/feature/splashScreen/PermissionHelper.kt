package com.flixclusive.feature.splashScreen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

fun Context.hasAllPermissionGranted(): Boolean {
    var allPermissionProvided = true

    val requiredPermissions = mutableListOf<String>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        requiredPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    for (permission in requiredPermissions) {
        val status = checkCallingOrSelfPermission(permission)

        if (status != PackageManager.PERMISSION_GRANTED) {
            allPermissionProvided = false
            break
        }
    }

    return allPermissionProvided
}