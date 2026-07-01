package com.flixclusive.feature.mobile.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.flixclusive.feature.mobile.onboarding.component.GrantedPermissionItem

internal object PermissionUtil {
    @RequiresApi(Build.VERSION_CODES.R)
    fun createManageStorageIntent(context: Context): Intent {
        return try {
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                "package:${context.packageName}".toUri(),
            )
        } catch (_: Exception) {
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        }
    }

    fun isStorageAccessGranted(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                Environment.isExternalStorageManager()
            }

            else -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    fun isNotificationsGranted(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT < 33 -> true

            else -> ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    @Suppress("DEPRECATION")
    fun isUnknownSourcesAllowed(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= 26 -> {
                context.packageManager.canRequestPackageInstalls()
            }

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

    fun getPreGrantedPermissions(
        context: Context,
        excludedPermissions: Set<String>,
    ): List<GrantedPermissionItem> {
        val pm = context.packageManager

        val requestedPermissions = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm
                    .getPackageInfo(
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
            }.distinct()
            .mapNotNull { permission ->
                val label = runCatching {
                    pm
                        .getPermissionInfo(permission, 0)
                        .loadLabel(pm)
                        .toString()
                }.getOrNull().orEmpty()

                val description = runCatching {
                    pm
                        .getPermissionInfo(permission, 0)
                        .loadDescription(pm)
                        .toString()
                }.getOrNull().orEmpty()

                if (label.isEmpty()) return@mapNotNull null
                if (label.startsWith("com.")) return@mapNotNull null

                GrantedPermissionItem(
                    label = label
                        .ifBlank { permission.substringAfterLast('.') }
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                    description = description
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                )
            }.sortedBy { it.label.lowercase() }
            .toList()
    }

    fun createUnknownSourcesIntent(packageName: String): Intent {
        return when {
            Build.VERSION.SDK_INT >= 26 -> Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                "package:$packageName".toUri(),
            )

            else -> Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }
}
