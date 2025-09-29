package com.flixclusive.feature.mobile.user.add.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import com.flixclusive.core.database.entity.user.User

internal object StateHoistingUtil {
    val LocalUserToAdd = compositionLocalOf { mutableStateOf(User(id = -1, name = "", image = 1)) }

    @Composable
    fun ProvideUserToAdd(
        user: MutableState<User>,
        content: @Composable () -> Unit
    ) {
        CompositionLocalProvider(
            LocalUserToAdd provides user,
            content = content,
        )
    }
}
