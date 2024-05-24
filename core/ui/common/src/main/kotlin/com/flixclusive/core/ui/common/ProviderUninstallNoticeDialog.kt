package com.flixclusive.core.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.gradle.entities.Author
import com.flixclusive.gradle.entities.Language
import com.flixclusive.gradle.entities.ProviderData
import com.flixclusive.gradle.entities.ProviderType
import com.flixclusive.gradle.entities.Status
import com.flixclusive.core.util.R as UtilR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderUninstallNoticeDialog(
    providerData: ProviderData,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val buttonMinHeight = 60.dp

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(10),
        ) {
            Box(
                modifier = Modifier
                    .height(180.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(10.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.warning),
                        contentDescription = stringResource(
                            id = UtilR.string.warning_content_description
                        ),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(10.dp)
                    )

                    Text(
                        text = buildAnnotatedString {
                            append(context.getString(UtilR.string.warning_uninstall_message_first_half))
                            append(" ")
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(providerData.name)
                            }
                            append("?")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.onMediumEmphasis(),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                            .padding(horizontal = 10.dp)
                    )

                    Row(
                        modifier = Modifier.weight(1F),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.onMediumEmphasis(),
                                contentColor = Color.Black
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1F)
                                .heightIn(min = buttonMinHeight)
                                .padding(5.dp)
                        ) {
                            Text(
                                text = stringResource(id = UtilR.string.uninstall),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(end = 2.dp)
                            )
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.onMediumEmphasis()
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1F)
                                .heightIn(min = buttonMinHeight)
                                .padding(5.dp)
                        ) {
                            Text(
                                text = stringResource(id = UtilR.string.cancel),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Light
                            )
                        }
                    }
                }
            }
        }
    }
}


@Preview
@Composable
private fun ProviderUninstallNoticeDialogPreview() {
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
            ProviderUninstallNoticeDialog(
                providerData = providerData,
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}