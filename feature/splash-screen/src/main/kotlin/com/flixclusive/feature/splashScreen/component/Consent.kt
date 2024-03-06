package com.flixclusive.feature.splashScreen.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.tv.util.focusOnInitialVisibility
import com.flixclusive.core.util.android.getActivity
import com.flixclusive.core.util.android.isTvMode
import androidx.compose.material3.Button as MobileButton
import androidx.compose.material3.ButtonDefaults as MobileButtonDefaults
import androidx.tv.material3.Button as TvButton
import androidx.tv.material3.ButtonDefaults as TvButtonDefaults
import com.flixclusive.core.util.R as UtilR

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Consent(
    header: String,
    consentContent: String,
    buttonLabel: String? = null,
    optInLabel: String? = null,
    hasOptInFeature: Boolean = false,
    goNext: (isOptIn: Boolean) -> Unit
) {
    val context = LocalContext.current.getActivity<ComponentActivity>()

    val buttonMinHeight = 60.dp

    var isOptIn by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .padding(horizontal = 5.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Divider(
            thickness = 0.5.dp,
            modifier = Modifier
                .padding(vertical = 8.dp)
        )

        Text(
            text = header,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
        )

        Text(
            text = consentContent,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Justify
            )
        )

        if (hasOptInFeature) {
            Surface(
                onClick = { isOptIn = !isOptIn },
                shape = ClickableSurfaceDefaults.shape(RectangleShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White.onMediumEmphasis(0.8F),
                    focusedContainerColor = Color.Transparent,
                    focusedContentColor = Color.White,
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1F),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(BorderStroke(1.dp, Color.White))
                ),
                modifier = Modifier
                    .padding(top = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isOptIn,
                        onCheckedChange = { isOptIn = !isOptIn },
                    )

                    Text(
                        text = stringResource(id = UtilR.string.privacy_notice_opt_in),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        if (context.isTvMode()) {
            TvButton(
                onClick = { goNext(isOptIn) },
                colors = TvButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = Color.White.onMediumEmphasis()
                ),
                shape = TvButtonDefaults.shape(MaterialTheme.shapes.medium),
                border = TvButtonDefaults.border(
                    focusedBorder = Border(
                        BorderStroke(3.dp, Color.White)
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = buttonMinHeight)
                    .focusOnInitialVisibility()
            ) {
                Text(
                    text = buttonLabel ?: stringResource(id = UtilR.string.understood),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                )
            }
        } else {
            MobileButton(
                onClick = { goNext(isOptIn) },
                colors = MobileButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = Color.White.onMediumEmphasis()
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = buttonMinHeight)
            ) {
                Text(
                    text = buttonLabel ?: stringResource(id = UtilR.string.understood),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ConsentPreview() {
    FlixclusiveTheme {
        Column {
            Text("FLIXCLUSIVE", fontSize = 30.sp)

            Consent(
                hasOptInFeature = true,
                header = stringResource(id = UtilR.string.privacy_notice),
                consentContent = stringResource(id = UtilR.string.privacy_notice_crash_log_sender),
                optInLabel = stringResource(id = UtilR.string.privacy_notice_opt_in),
                goNext = {}
            )
        }
    }
}