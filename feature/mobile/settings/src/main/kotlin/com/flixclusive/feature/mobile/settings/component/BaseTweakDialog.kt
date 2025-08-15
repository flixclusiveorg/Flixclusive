package com.flixclusive.feature.mobile.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.strings.R
import com.flixclusive.core.ui.common.dialog.CustomBaseAlertDialog
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle

@Composable
internal fun BaseTweakDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val buttonMinHeight = 50.dp
    val buttonShape = MaterialTheme.shapes.medium

    CustomBaseAlertDialog(
        onDismiss = onDismissRequest,
        action = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier =
                    Modifier
                        .padding(horizontal = 10.dp)
                        .padding(bottom = 10.dp),
            ) {
                Button(
                    enabled = onConfirm != null,
                    onClick = {
                        onConfirm?.invoke()
                        onDismissRequest()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    shape = buttonShape,
                    modifier =
                        Modifier
                            .weight(1F)
                            .heightIn(min = getAdaptiveDp(buttonMinHeight)),
                ) {
                    Text(
                        text = stringResource(id = R.string.confirm),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier =
                            Modifier
                                .padding(end = 2.dp),
                    )
                }

                Button(
                    onClick = onDismissRequest,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black,
                        ),
                    shape = buttonShape,
                    modifier =
                        Modifier
                            .weight(1F)
                            .heightIn(min = getAdaptiveDp(buttonMinHeight)),
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Light,
                    )
                }
            }
        },
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(
                text = title,
                style =
                    getAdaptiveTextStyle(
                        style = TypographyStyle.Headline,
                        mode = TextStyleMode.Emphasized,
                        size = 20.sp,
                    ),
                modifier =
                    Modifier
                        .padding(10.dp),
            )

            content()
        }
    }
}
