package com.flixclusive.feature.splashScreen.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
import com.flixclusive.core.theme.util.TvModeChecker.isTvMode
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.tv.util.focusOnInitialVisibility
import com.flixclusive.core.util.android.getActivity
import androidx.compose.material3.Button as MobileButton
import androidx.compose.material3.ButtonDefaults as MobileButtonDefaults
import androidx.tv.material3.Button as TvButton
import androidx.tv.material3.ButtonDefaults as TvButtonDefaults
import com.flixclusive.core.locale.R as LocaleR

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun Consent(
    modifier: Modifier = Modifier,
    header: String,
    consentContent: String,
    buttonLabel: String? = null,
    optInLabel: String? = null,
    goNext: (isOptIn: Boolean) -> Unit
) {
    val context = LocalContext.current.getActivity<ComponentActivity>()
    val buttonMinHeight = 60.dp

    var isOptIn by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .padding(
                horizontal = 5.dp,
                vertical = 8.dp
            ),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalDivider(
            modifier = Modifier
                .padding(vertical = 8.dp),
            thickness = 0.5.dp
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1F, fill = false)
                .padding(bottom = 15.dp)
        ) {
            Text(
                text = header,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                ),
                modifier = Modifier
                    .padding(bottom = 16.dp)
            )

            Text(
                text = consentContent,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Justify
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            )
        }


        if (optInLabel != null) {
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
                        text = optInLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // TODO("Remove `context.isTvMode()` in here after creating TV UI's own splash screen")
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
                    text = buttonLabel ?: stringResource(id = LocaleR.string.understood),
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
                    text = buttonLabel ?: stringResource(id = LocaleR.string.understood),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview
@Composable
private fun ConsentPreview() {
    FlixclusiveTheme {
        Surface {
            Consent(
                header = stringResource(id = LocaleR.string.privacy_notice),
                consentContent = """
                     Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean vel laoreet dui. In hac habitasse platea dictumst. Maecenas neque dui, pretium ac dui eu, condimentum sollicitudin arcu. Sed ac interdum sem, consequat posuere enim. Proin vitae lacus quis augue porttitor luctus. Proin consequat lobortis neque, ac malesuada massa lobortis ut. Vestibulum vel augue consequat nulla fringilla porttitor sit amet quis mauris. Nunc et nisi in sapien interdum euismod id in urna. Curabitur pretium malesuada diam, vel accumsan velit commodo ut. Aenean ac efficitur ligula, vel dictum felis. Nunc placerat tortor nec metus venenatis, a malesuada neque volutpat.

Nam quis efficitur nulla. Nulla venenatis non augue et viverra. Vivamus sit amet vestibulum mi, ac volutpat dolor. Aenean cursus, ex ornare lacinia semper, est lectus fermentum quam, quis porta felis turpis faucibus nulla. In hac habitasse platea dictumst. Vestibulum pulvinar fermentum mi sit amet eleifend. Fusce euismod mi nec finibus maximus. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Nullam venenatis massa nec tellus dignissim, et facilisis risus dapibus.

Integer efficitur viverra mauris. Mauris bibendum ipsum quis quam vehicula interdum. Ut aliquam mauris tempor iaculis pulvinar. Integer porttitor, mi vitae congue mollis, sapien libero rhoncus velit, id semper odio risus ullamcorper ligula. Suspendisse potenti. Nam tincidunt elit in ex ornare venenatis. Etiam laoreet enim sed nibh suscipit mattis. Proin mauris nunc, molestie id consequat ac, convallis vel neque. 
                """.trimIndent(),
                optInLabel = stringResource(id = LocaleR.string.privacy_notice_opt_in),
                goNext = {}
            )
        }
    }
}