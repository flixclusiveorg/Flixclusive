package com.flixclusive.feature.mobile.provider.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import dev.jeziellago.compose.markdowntext.MarkdownText
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

@Composable
internal fun ChangeLogsDialog(
    changeLogs: String,
    changeLogsHeaderImage: String?,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth(0.9F)
                .fillMaxHeight(0.8F)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                ) {
                    Icon(
                        painter = painterResource(id = UiCommonR.drawable.round_close_24),
                        contentDescription = stringResource(id = UtilR.string.cancel)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    changeLogsHeaderImage?.let {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(it)
                                .crossfade(true)
                                .build(),
                            imageLoader = LocalContext.current.imageLoader,
                            contentDescription = stringResource(id = UtilR.string.change_logs_image_content_desc),
                            modifier = Modifier
                                .size(100.dp)
                                .padding(top = 10.dp, bottom = 5.dp)
                        )
                    }

                    Text(
                        text = stringResource(UtilR.string.change_logs),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black
                        ),
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                    )

                }

                MarkdownText(
                    markdown = changeLogs,
                    isTextSelectable = true,
                    linkColor = Color(0xFF5890FF),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = LocalContentColor.current,
                    ),
                    imageLoader = LocalContext.current.imageLoader,
                    onLinkClicked = uriHandler::openUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }
        }
    }
}