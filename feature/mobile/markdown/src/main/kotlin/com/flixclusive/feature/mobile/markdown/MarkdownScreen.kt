package com.flixclusive.feature.mobile.markdown

import android.text.util.Linkify
import android.util.Patterns
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
import coil3.imageLoader
import com.flixclusive.core.presentation.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.dialog.TextAlertDialog
import com.flixclusive.core.ui.common.navigation.navigator.GoBackAction
import com.flixclusive.core.ui.mobile.component.topbar.CommonTopBar
import com.flixclusive.core.ui.mobile.component.topbar.CommonTopBarDefaults.DefaultTopBarHeight
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

    Box(
        modifier =
            Modifier
                .fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .padding(top = DefaultTopBarHeight)
                    .verticalScroll(rememberScrollState()),
        ) {
            MarkdownText(
                markdown = description,
                isTextSelectable = true,
                linkColor = Color(0xFF5890FF),
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        color = LocalContentColor.current,
                    ),
                linkifyMask = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES,
                imageLoader = LocalContext.current.imageLoader,
                onLinkClicked = {
                    if (isValidUrl(it)) {
                        linkToOpen = it
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
            )

            Spacer(
                modifier =
                    Modifier
                        .navigationBarsPadding()
                        .height(10.dp),
            )
        }

        CommonTopBar(
            title = title,
            onNavigate = navigator::goBack,
        )
    }

    if (linkToOpen != null) {
        TextAlertDialog(
            label = stringResource(id = LocaleR.string.heads_up),
            description = stringResource(id = LocaleR.string.not_trusted_url),
            confirmButtonLabel = stringResource(id = LocaleR.string.proceed),
            onConfirm = { uriHandler.openUri(linkToOpen!!) },
            onDismiss = { linkToOpen = null },
        )
    }
}

@Preview
@Composable
private fun ProviderWhatsNewScreenPreview() {
    FlixclusiveTheme {
        Surface {
            MarkdownScreen(
                navigator =
                    object : GoBackAction {
                        override fun goBack() {}
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
