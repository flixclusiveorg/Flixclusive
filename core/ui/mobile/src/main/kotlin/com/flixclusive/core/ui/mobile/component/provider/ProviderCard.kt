package com.flixclusive.core.ui.mobile.component.provider

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.gradle.entities.Author
import com.flixclusive.gradle.entities.Language
import com.flixclusive.gradle.entities.ProviderData
import com.flixclusive.gradle.entities.ProviderType
import com.flixclusive.gradle.entities.Status
import com.flixclusive.core.util.R as UtilR

enum class ProviderCardState {
    NotInstalled,
    Installing,
    Installed,
}

@Composable
fun ProviderCard(
    modifier: Modifier = Modifier,
    providerData: ProviderData,
    state: ProviderCardState,
    onClick: () -> Unit,
) {
    val context = LocalContext.current

    Card(
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(
                    horizontal = 15.dp,
                    vertical = 10.dp
                )
        ) {
            TopCardContent(
                isDraggable = false,
                providerData = providerData
            )

            Divider(
                thickness = 0.5.dp,
                modifier = Modifier
                    .padding(vertical = 15.dp)
            )

            providerData.description?.let {
                Text(
                    text = it,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Normal,
                        color = LocalContentColor.current.onMediumEmphasis(),
                        fontSize = 13.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )
            }

            ElevatedButton(
                onClick = onClick,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .height(50.dp)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = state,
                    label = ""
                ) {
                    val textLabel = remember(it) {
                        when (it) {
                            ProviderCardState.Installed -> context.getString(UtilR.string.uninstall)
                            else -> context.getString(UtilR.string.install)
                        }
                    }

                    if (it == ProviderCardState.Installing) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .size(20.dp)
                        )
                    } else {
                        Text(
                            text = textLabel,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ProviderCardPreview() {
    val providerData = ProviderData(
        authors = listOf(Author("FLX")),
        repositoryUrl = null,
        buildUrl = null,
        changelog = null,
        changelogMedia = null,
        versionName = "1.0.0",
        versionCode = 10000,
        description = "lorem ipsum lorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsum",
        iconUrl = null,
        language = Language.Multiple,
        name = "123Movies",
        providerType = ProviderType.All,
        status = Status.Working
    )

    FlixclusiveTheme {
        Surface {
            ProviderCard(
                providerData = providerData,
                state = ProviderCardState.Installed
            ) {

            }
        }
    }
}