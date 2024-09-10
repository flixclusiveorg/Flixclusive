package com.flixclusive.feature.mobile.markdown

import android.text.util.Linkify
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.imageLoader
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.COMMON_TOP_BAR_HEIGHT
import com.flixclusive.core.ui.common.CommonNoticeDialog
import com.flixclusive.core.ui.common.CommonTopBar
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.navigation.MarkdownNavArgs
import com.ramcosta.composedestinations.annotation.Destination
import dev.jeziellago.compose.markdowntext.MarkdownText
import com.flixclusive.core.util.R as UtilR

@Destination(
    navArgsDelegate = MarkdownNavArgs::class
)
@Composable
fun MarkdownScreen(
    navigator: GoBackAction,
    args: MarkdownNavArgs
) {
    val uriHandler = LocalUriHandler.current
    var linkToOpen by rememberSaveable { mutableStateOf<String?>(null) }

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
            MarkdownText(
                markdown = args.description,
                isTextSelectable = true,
                linkColor = Color(0xFF5890FF),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = LocalContentColor.current,
                ),
                linkifyMask = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES,
                imageLoader = LocalContext.current.imageLoader,
                onLinkClicked = {
                    linkToOpen = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            
            Spacer(
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(10.dp)
            )
        }

        CommonTopBar(
            headerTitle = args.title,
            onNavigationIconClick = navigator::goBack
        )
    }

    if (linkToOpen != null) {
        CommonNoticeDialog(
            label = stringResource(id = UtilR.string.heads_up),
            description = stringResource(id = UtilR.string.not_trusted_url),
            confirmButtonLabel = stringResource(id = UtilR.string.proceed),
            onConfirm = { uriHandler.openUri(linkToOpen!!) },
            onDismiss = { linkToOpen = null }
        )
    }
}

@Preview
@Composable
private fun ProviderWhatsNewScreenPreview() {
    FlixclusiveTheme {
        Surface {
            MarkdownScreen(
                navigator = object : GoBackAction {
                    override fun goBack() {}
                },
                args = MarkdownNavArgs(
                    title = "2.0.0",
                    description = """
                        # Flixclusive
                        
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                        Link: https://example.com
                    """.trimIndent()
                )
            )
        }
    }
}