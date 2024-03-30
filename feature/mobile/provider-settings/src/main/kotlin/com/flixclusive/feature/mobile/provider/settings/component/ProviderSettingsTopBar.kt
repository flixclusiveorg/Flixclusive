package com.flixclusive.feature.mobile.provider.settings.component


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.core.util.R
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
internal fun ProviderSettingsTopBar(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    repositoryUrl: String?,
    onNavigationIconClick: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

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
                        contentDescription = stringResource(R.string.navigate_up)
                    )
                }

                Text(
                    text = stringResource(id = R.string.repository),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1F)
                        .padding(horizontal = 15.dp)
                )

                repositoryUrl?.let {
                    IconButton(
                        onClick = { uriHandler.openUri(it) }
                    ) {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.web_browser),
                            contentDescription = stringResource(R.string.open_web_icon),
                        )
                    }
                }
            }
        }
    }
}