package com.flixclusive.feature.mobile.user.add.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.flixclusive.core.ui.common.user.UserAvatarDefaults.AVATARS_IMAGE_COUNT
import com.flixclusive.model.database.User
import kotlin.random.Random

internal object StateHoistingUtil {
    private val defaultUser = User(
        image = Random.nextInt(AVATARS_IMAGE_COUNT)
    )
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