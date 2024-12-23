package com.flixclusive.feature.mobile.user.add.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.flixclusive.model.database.User

internal object StateHoistingUtil {
    private val defaultUser = User(image = -1)
    val LocalUserToAdd = compositionLocalOf { mutableStateOf(defaultUser) }

    @Composable
    fun ProvideUserToAdd(
        content: @Composable () -> Unit
    ) {
        CompositionLocalProvider(
            LocalUserToAdd provides remember { mutableStateOf(defaultUser) },
            content = content,
        )
    }
}