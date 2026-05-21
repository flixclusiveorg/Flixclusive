package com.flixclusive.feature.mobile.provider.details.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.flixclusive.core.presentation.mobile.components.ImageWithSmallPlaceholder
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.model.provider.Author
import com.flixclusive.core.drawables.R as UiCommonR

@Composable
internal fun AuthorCard(
    author: Author,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(
                enabled = author.socialLink != null,
                onClick = {
                    author.socialLink?.let(uriHandler::openUri)
                }
            )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 5.dp, horizontal = 5.dp)
                .align(Alignment.CenterStart)
        ) {
            ImageWithSmallPlaceholder(
                modifier = Modifier.size(35.dp),
                placeholderSize = 35.dp,
                urlImage = author.image,
                placeholder = painterResource(UiCommonR.drawable.profile_placeholder),
                contentDescription = author.name,
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = author.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                author.socialLink?.let {
                    Text(
                        text = remember { it.toDisplayUrl() },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (author.socialLink != null) {
                Icon(
                    painter = painterResource(UiCommonR.drawable.open_url),
                    contentDescription = null,
                    tint = LocalContentColor.current.copy(0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun String.toDisplayUrl(
    removeQuery: Boolean = true,
    removeFragment: Boolean = true
): String {
    return runCatching {
        val uri = if (this.contains("://")) this.toUri() else "http://$this".toUri()

        val host = uri.host
            ?.removePrefix("www.")
            ?: return this

        val path = uri.path
            ?.takeIf { it != "/" }
            ?.removeSuffix("/")
            ?: ""

        val query = if (removeQuery) "" else uri.query?.let { "?$it" } ?: ""
        val fragment = if (removeFragment) "" else uri.fragment?.let { "#$it" } ?: ""

        "$host$path$query$fragment"
    }.getOrElse {
        // fallback for malformed URLs
        this
            .substringAfter("://", this)
            .removePrefix("www.")
            .trimEnd('/')
    }
}

@Preview
@Composable
private fun AuthorCardPreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxWidth()
        ) {
            AuthorCard(
                author = Author(
                    name = "John Doe",
                    image = null,
                    socialLink = "https://github.com/flixclusive-provider"
                ),
            )
        }
    }
}
