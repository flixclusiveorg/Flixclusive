package com.flixclusive.feature.mobile.provider.whats_new

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.imageLoader
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.COMMON_TOP_BAR_HEIGHT
import com.flixclusive.core.ui.common.CommonTopBar
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.navigation.ProviderInfoScreenNavArgs
import com.flixclusive.core.ui.common.util.DummyDataForPreview.getDummyProviderData
import com.ramcosta.composedestinations.annotation.Destination
import dev.jeziellago.compose.markdowntext.MarkdownText
import com.flixclusive.core.theme.R as ThemeR

@Destination(
    navArgsDelegate = ProviderInfoScreenNavArgs::class
)
@Composable
fun ProviderWhatsNewScreen(
    navigator: GoBackAction,
    args: ProviderInfoScreenNavArgs
) {
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = COMMON_TOP_BAR_HEIGHT)
                .verticalScroll(rememberScrollState())
        ) {
            args.providerData.changelog?.let {
                MarkdownText(
                    markdown = it,
                    isTextSelectable = true,
                    linkColor = Color(0xFF5890FF),
                    fontResource = ThemeR.font.space_grotesk_medium,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = LocalContentColor.current,
                    ),
                    imageLoader = LocalContext.current.imageLoader,
                    onLinkClicked = uriHandler::openUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }

        CommonTopBar(
            headerTitle = args.providerData.name,
            onNavigationIconClick = navigator::goBack
        )
    }
}

@Preview
@Composable
private fun ProviderWhatsNewScreenPreview() {
    FlixclusiveTheme {
        Surface {
            ProviderWhatsNewScreen(
                navigator = object : GoBackAction {
                    override fun goBack() {}
                },
                args = ProviderInfoScreenNavArgs(
                    providerData = getDummyProviderData()
                )
            )
        }
    }
}