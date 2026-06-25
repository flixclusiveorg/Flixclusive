package com.flixclusive.feature.mobile.provider.details.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.imageLoader
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.provider.details.R
import dev.jeziellago.compose.markdowntext.MarkdownText
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun ProviderDetailsWhatsNew(
    changelogs: String,
    latestVersion: String,
    onViewChangelogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            SectionLabel(text = stringResource(id = LocaleR.string.whats_new))

            Text(
                text = "v$latestVersion",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = LocalContentColor.current.copy(0.5f)
                ),
            )
        }

        MarkdownText(
            markdown = changelogs,
            maxLines = 3,
            linkifyMask = 0,
            imageLoader = context.imageLoader,
            truncateOnTextOverflow = true,
            style = MaterialTheme.typography.bodySmall.copy(
                color = LocalContentColor.current.copy(0.8f),
            ),
            modifier = Modifier
                .fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.label_view_full_changelogs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable {
                    onViewChangelogs()
                }
        )
    }
}

@Preview
@Composable
private fun ProviderDetailsWhatsNewPreview() {
    FlixclusiveTheme {
        Surface {
            ProviderDetailsWhatsNew(
                latestVersion = "v1.2.3",
                onViewChangelogs = {},
                changelogs = """
                        - Added new feature X
                        - Improved performance of Y
                        - Fixed bug Z
                """.trimIndent(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
