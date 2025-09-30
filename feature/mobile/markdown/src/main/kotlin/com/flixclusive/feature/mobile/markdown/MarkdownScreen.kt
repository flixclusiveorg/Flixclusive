package com.flixclusive.feature.mobile.markdown

import android.text.util.Linkify
import android.util.Patterns
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.imageLoader
import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.presentation.mobile.components.material3.dialog.TextAlertDialog
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBar
import com.flixclusive.core.presentation.mobile.components.material3.topbar.rememberEnterAlwaysScrollBehavior
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.ramcosta.composedestinations.annotation.Destination
import dev.jeziellago.compose.markdowntext.MarkdownText
import com.flixclusive.core.strings.R as LocaleR

private fun isValidUrl(url: String): Boolean {
    return Patterns.WEB_URL.matcher(url).matches()
}

@Destination
@Composable
internal fun MarkdownScreen(
    navigator: GoBackAction,
    title: String,
    description: String,
) {
    val uriHandler = LocalUriHandler.current
    var linkToOpen by rememberSaveable { mutableStateOf<String?>(null) }

    val enterAlwaysScrollBehavior = rememberEnterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(enterAlwaysScrollBehavior.nestedScrollConnection),
        topBar = {
            CommonTopBar(
                title = title,
                onNavigate = navigator::goBack,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState()),
        ) {
            MarkdownText(
                markdown = description,
                isTextSelectable = true,
                linkColor = Color(0xFF5890FF),
                style = MaterialTheme.typography.bodySmall
                    .let {
                        it.copy(
                            color = LocalContentColor.current,
                            lineHeight = it.lineHeight * 0.85f,
                        )
                    }.asAdaptiveTextStyle(size = 12.sp),
                linkifyMask = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES,
                imageLoader = LocalContext.current.imageLoader,
                onLinkClicked = {
                    if (isValidUrl(it)) {
                        linkToOpen = it
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(10.dp),
            )
        }
    }

    if (linkToOpen != null) {
        TextAlertDialog(
            title = stringResource(id = LocaleR.string.heads_up),
            message = stringResource(id = LocaleR.string.not_trusted_url),
            confirmButtonLabel = stringResource(id = LocaleR.string.proceed),
            onConfirm = { uriHandler.openUri(linkToOpen!!) },
            onDismiss = { linkToOpen = null },
        )
    }
}

@Preview
@Composable
private fun MarkdownScreenPreview() {
    FlixclusiveTheme {
        Surface {
            MarkdownScreen(
                navigator = object : GoBackAction {
                    override fun goBack() = Unit
                },
                title = "2.0.0",
                description =
                    """
                    # Flixclusive

                    Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                    Link: https://example.com
                    """.trimIndent(),
            )
        }
    }
}
