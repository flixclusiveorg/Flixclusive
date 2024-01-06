package com.flixclusive.core.theme

import androidx.compose.runtime.Composable
import com.flixclusive.core.theme.mobile.FlixclusiveMobileTheme
import com.flixclusive.core.theme.tv.FlixclusiveTvTheme

@Composable
fun FlixclusiveTheme(
    isTv: Boolean = false,
    useDarkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    if(isTv) {
        FlixclusiveTvTheme(
            useDarkTheme = useDarkTheme,
            content = content
        )
    } else {
        FlixclusiveMobileTheme(
            useDarkTheme = useDarkTheme,
            content = content
        )
    }
}