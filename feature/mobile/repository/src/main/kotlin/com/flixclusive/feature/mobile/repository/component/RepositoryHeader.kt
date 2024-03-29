package com.flixclusive.feature.mobile.repository.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.component.ImageWithSmallPlaceholder
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.feature.mobile.repository.R
import com.flixclusive.gradle.entities.Repository
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

@Composable
fun RepositoryHeader(
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
                contentDescId = UtilR.string.owner_avatar_content_desc
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
            CustomOutlineButton(
                onClick = { uriHandler.openUri(repository.url) },
                iconId = R.drawable.web_browser,
                label = stringResource(id = UtilR.string.open_web_icon),
                modifier = Modifier
                    .weight(1F)
            )

            CustomOutlineButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(repository.url))
                    toggleSnackbar(UiText.StringResource(UtilR.string.copied_link))
                },
                iconId = UiCommonR.drawable.round_content_copy_24,
                label = stringResource(id = UtilR.string.copy_link),
                modifier = Modifier
                    .weight(1F)
            )
        }
    }
}

@Composable
private fun CustomOutlineButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    @DrawableRes iconId: Int,
    label: String
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface.onMediumEmphasis(0.8F)
        ),
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.onMediumEmphasis(),
        ),
        contentPadding = PaddingValues(vertical = 15.dp),
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = label,
            modifier = Modifier
                .size(20.dp)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier
                .padding(start = 5.dp)
        )
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
                    "https://github.com/rhenwinch/providers",
                    ""
                ),
                toggleSnackbar = {}
            )
        }
    }
}