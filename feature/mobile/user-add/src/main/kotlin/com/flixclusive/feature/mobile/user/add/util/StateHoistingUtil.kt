package com.flixclusive.feature.mobile.user.add.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import com.flixclusive.core.database.entity.User
import com.flixclusive.core.ui.common.util.DummyDataForPreview.getDummyUser

internal object StateHoistingUtil {
    val LocalUserToAdd = compositionLocalOf { mutableStateOf(getDummyUser()) }

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
