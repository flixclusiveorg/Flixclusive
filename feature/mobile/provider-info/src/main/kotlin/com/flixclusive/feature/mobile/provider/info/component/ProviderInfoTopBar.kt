package com.flixclusive.feature.mobile.provider.info.component


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun ProviderInfoTopBar(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    providerName: String,
    onNavigationIconClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .height(65.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigationIconClick) {
                    Icon(
                        painter = painterResource(UiCommonR.drawable.left_arrow),
                        contentDescription = stringResource(LocaleR.string.navigate_up)
                    )
                }

                Spacer(
                    modifier = Modifier
                        .weight(1F)
                        .padding(horizontal = 15.dp)
                )

                IconButton(onClick = onSettingsClick) {
                    Icon(
                        painter = painterResource(UiCommonR.drawable.provider_settings),
                        contentDescription = stringResource(LocaleR.string.open_web_icon),
                    )
                }
            }
        }
    }
}