package com.flixclusive.feature.mobile.provider.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

@Composable
internal fun ProvidersTopBar(
    isVisible: Boolean,
    onActionClick: () -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .drawBehind {
                    drawRect(surfaceColor)
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .heightIn(min = 65.dp)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = UtilR.string.providers),
                    style = MaterialTheme.typography.headlineMedium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1F)
                        .padding(horizontal = 15.dp)
                )

                IconButton(
                    onClick = onActionClick
                ) {
                    Icon(
                        painter = painterResource(id = UiCommonR.drawable.search_outlined),
                        contentDescription = stringResource(id = UtilR.string.search_for_providers)
                    )
                }
            }
        }
    }
}


@Preview
@Composable
private fun TopBarPreview() {
    FlixclusiveTheme {
        Surface {
            Scaffold(
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    ProvidersTopBar(
                        isVisible = true,
                        onActionClick = {}
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { /*TODO*/ },
                        containerColor = MaterialTheme.colorScheme.surface,
                        text = {
                            Text(text = stringResource(UtilR.string.add_provider))
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = UiCommonR.drawable.round_add_24),
                                contentDescription = stringResource(UtilR.string.add_provider)
                            )
                        }
                    )
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it)
                ) {

                }
            }
        }
    }
}