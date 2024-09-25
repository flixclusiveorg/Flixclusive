package com.flixclusive.core.ui.mobile.component.provider

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
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
import com.flixclusive.core.ui.common.util.DummyDataForPreview
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.model.provider.ProviderData
import com.flixclusive.core.locale.R as LocaleR

enum class ProviderInstallationStatus {
    NotInstalled,
    Installing,
    Installed,
    Outdated;

    val isNotInstalled: Boolean
        get() = this == NotInstalled
    val isInstalled: Boolean
        get() = this == Installed
    val isOutdated: Boolean
        get() = this == Outdated
    val isInstalling: Boolean
        get() = this == Installing
}

@Composable
fun ProviderCard(
    modifier: Modifier = Modifier,
    providerData: ProviderData,
    status: ProviderInstallationStatus,
    onClick: () -> Unit,
) {
    val context = LocalContext.current

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        ),
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

            HorizontalDivider(
                modifier = Modifier
                    .padding(vertical = 15.dp),
                thickness = 0.5.dp
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
                    targetState = status,
                    label = ""
                ) {
                    val textLabel = remember(it) {
                        when (it) {
                            ProviderInstallationStatus.Installed -> context.getString(LocaleR.string.uninstall)
                            ProviderInstallationStatus.Outdated -> context.getString(LocaleR.string.update_label)
                            else -> context.getString(LocaleR.string.install)
                        }
                    }

                    if (it == ProviderInstallationStatus.Installing) {
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
    val providerData = DummyDataForPreview.getDummyProviderData()

    FlixclusiveTheme {
        Surface {
            ProviderCard(
                providerData = providerData,
                status = ProviderInstallationStatus.Installed
            ) {

            }
        }
    }
}