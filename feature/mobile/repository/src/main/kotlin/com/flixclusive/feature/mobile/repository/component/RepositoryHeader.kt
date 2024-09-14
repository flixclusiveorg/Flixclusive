package com.flixclusive.feature.mobile.repository.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.mobile.component.ImageWithSmallPlaceholder
import com.flixclusive.core.ui.mobile.component.provider.ButtonWithCircularProgressIndicator
import com.flixclusive.core.locale.UiText
import com.flixclusive.model.provider.Repository
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun RepositoryHeader(
    repository: Repository,
    toggleSnackbar: (UiText) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    Column(
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImageWithSmallPlaceholder(
                modifier = Modifier.size(120.dp),
                placeholderModifier = Modifier.size(70.dp),
                urlImage = if (repository.url.contains("github")) "https://github.com/${repository.owner}.png" else null,
                placeholderId = UiCommonR.drawable.repository,
                contentDescId = LocaleR.string.owner_avatar_content_desc
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
                modifier = Modifier
                    .weight(1F)
            ) {
                Text(
                    text = repository.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Text(
                    text = repository.owner,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            ButtonWithCircularProgressIndicator(
                onClick = { uriHandler.openUri(repository.url) },
                iconId = UiCommonR.drawable.web_browser,
                label = stringResource(id = LocaleR.string.open_web_icon),
                modifier = Modifier
                    .weight(1F)
            )

            ButtonWithCircularProgressIndicator(
                onClick = {
                    clipboardManager.setText(AnnotatedString(repository.url))
                    toggleSnackbar(UiText.StringResource(LocaleR.string.copied_link))
                },
                iconId = UiCommonR.drawable.round_content_copy_24,
                label = stringResource(id = LocaleR.string.copy_link),
                modifier = Modifier
                    .weight(1F)
            )
        }
    }
}


@Preview
@Composable
private fun RepositoryHeaderPreview() {
    FlixclusiveTheme {
        Surface {
            RepositoryHeader(
                repository = Repository(
                    "rhenwinch",
                    "Flixclusive plugins-templates",
                    "https://github.com/flixclusiveorg/providers",
                    ""
                ),
                toggleSnackbar = {}
            )
        }
    }
}