package com.flixclusive.core.presentation.mobile.components.provider

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.R
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme

/**
 * A button that shows a progress indicator when loading.
 *
 * Mainly used for provider install buttons
 *
 * @param onClick The action to perform when the button is clicked.
 * @param iconId The resource id of the icon to display in the button.
 * @param label The text to display in the button.
 * @param modifier The modifier to be applied to the button.
 * @param isLoading Whether to show the progress indicator or the button content.
 * @param enabled Whether the button is enabled or not.
 * @param emphasize Whether to use a filled button style or an outlined button style.
 * @param indicatorSize The size of the progress indicator.
 * @param contentPadding The padding values for the button content.
 * */
@Composable
fun ButtonWithProgress(
    onClick: () -> Unit,
    @DrawableRes iconId: Int,
    label: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    emphasize: Boolean = false,
    indicatorSize: Dp = 20.dp,
    contentPadding: PaddingValues = PaddingValues(vertical = 15.dp),
) {
    if (emphasize) {
        Button(
            enabled = enabled,
            onClick = onClick,
            contentPadding = contentPadding,
            shape = MaterialTheme.shapes.small,
            modifier = modifier,
        ) {
            IconLabel(
                iconId = iconId,
                label = label,
                isLoading = isLoading,
                indicatorSize = indicatorSize,
            )
        }
    } else {
        OutlinedButton(
            enabled = enabled,
            onClick = onClick,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface.copy(0.8F),
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
            ),
            contentPadding = contentPadding,
            shape = MaterialTheme.shapes.small,
            modifier = modifier,
        ) {
            IconLabel(
                iconId = iconId,
                label = label,
                isLoading = isLoading,
                indicatorSize = indicatorSize,
            )
        }
    }
}

@Composable
private fun IconLabel(
    @DrawableRes iconId: Int,
    label: String,
    isLoading: Boolean,
    indicatorSize: Dp = 20.dp,
) {
    AnimatedContent(
        targetState = isLoading,
        label = "IconLabelAnimation",
    ) { state ->
        if (state) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(indicatorSize),
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = iconId),
                    contentDescription = label,
                    dp = 16.dp,
                )

                Text(
                    text = label,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun ButtonWithProgressBasePreview() {
    FlixclusiveTheme {
        Surface {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                ButtonWithProgress(
                    onClick = { },
                    iconId = R.drawable.round_wifi_24,
                    label = "WiFi On",
                    isLoading = true,
                    emphasize = true,
                    modifier = Modifier
                        .padding(16.dp),
                )

                ButtonWithProgress(
                    onClick = { },
                    iconId = R.drawable.right_arrow,
                    label = "Right Arrow",
                    isLoading = false,
                    emphasize = true,
                    modifier = Modifier
                        .padding(16.dp),
                )

                ButtonWithProgress(
                    onClick = { },
                    iconId = R.drawable.round_wifi_off_24,
                    label = "WiFi Off",
                    isLoading = true,
                    emphasize = false,
                    modifier = Modifier
                        .padding(16.dp),
                )

                ButtonWithProgress(
                    onClick = { },
                    iconId = R.drawable.filter,
                    label = "Filter",
                    isLoading = false,
                    emphasize = false,
                    modifier = Modifier
                        .padding(16.dp),
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun ButtonWithProgressCompactLandscapePreview() {
    ButtonWithProgressBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun ButtonWithProgressMediumPortraitPreview() {
    ButtonWithProgressBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun ButtonWithProgressMediumLandscapePreview() {
    ButtonWithProgressBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun ButtonWithProgressExtendedPortraitPreview() {
    ButtonWithProgressBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun ButtonWithProgressExtendedLandscapePreview() {
    ButtonWithProgressBasePreview()
}
