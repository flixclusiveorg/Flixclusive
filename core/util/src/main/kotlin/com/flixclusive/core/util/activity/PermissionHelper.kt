package com.flixclusive.core.util.activity

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

    for (permission in requiredPermissions){

        val status = checkCallingOrSelfPermission(permission)

        if (status != PackageManager.PERMISSION_GRANTED){
            allPermissionProvided = false
            break
        }
    }

    return allPermissionProvided
}