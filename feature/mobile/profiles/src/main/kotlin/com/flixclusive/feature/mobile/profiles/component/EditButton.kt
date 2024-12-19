package com.flixclusive.feature.mobile.profiles.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveSemiEmphasizedLabel
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
internal fun EditButton(
    buttonSize: Dp,
    iconSize: Dp,
    spacing: Dp,
    fontSize: TextUnit,
    contentPadding: PaddingValues = PaddingValues(vertical = 0.dp, horizontal = 5.dp),
    isEditing: Boolean = false,
    enabled: Boolean = true,
    onEdit: () -> Unit
) {
    val mediumEmphasisColor = LocalContentColor.current.onMediumEmphasis()
    OutlinedButton(
        enabled = enabled,
        onClick = onEdit,
        shape = MaterialTheme.shapes.extraSmall,
        contentPadding = contentPadding,
        border = ButtonDefaults.outlinedButtonBorder(enabled).copy(
            width = 0.5.dp,
            brush = SolidColor(mediumEmphasisColor),
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = mediumEmphasisColor,
            disabledContentColor = mediumEmphasisColor.copy(alpha = 0.5F)
        ),
        modifier = Modifier.height(buttonSize)
    ) {
        AnimatedContent(
            targetState = isEditing,
            label = "Edit Button"
        ) { state ->
            if (!state) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(UiCommonR.drawable.edit),
                        contentDescription = stringResource(LocaleR.string.edit_profile_button_desc),
                        modifier = Modifier
                            .size(iconSize)
                    )

                    Text(
                        text = stringResource(LocaleR.string.edit),
                        style = getAdaptiveSemiEmphasizedLabel(fontSize).copy(
                            color = LocalContentColor.current
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Icon(
                    painter = painterResource(UiCommonR.drawable.round_close_24),
                    contentDescription = stringResource(LocaleR.string.edit_profile_cancel_button_desc),
                    modifier = Modifier
                        .size(iconSize)
                )
            }
        }
    }
}