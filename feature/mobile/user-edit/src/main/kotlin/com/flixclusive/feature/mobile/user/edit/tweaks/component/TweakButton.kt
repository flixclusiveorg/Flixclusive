package com.flixclusive.feature.mobile.user.edit.tweaks.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.material3.dialog.TextAlertDialog
import com.flixclusive.core.presentation.mobile.extensions.fillMaxAdaptiveWidth
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.feature.mobile.user.edit.tweaks.ProfileTweakUI
import com.flixclusive.feature.mobile.user.edit.tweaks.TweakUiUtil.DefaultShape
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun TweakButton(
    tweak: ProfileTweakUI.Button
) {
    val needsDialog = tweak is ProfileTweakUI.Dialog || tweak.needsConfirmation

    var showDialog by remember { mutableStateOf(false) }

    if (needsDialog && showDialog) {
        if(tweak is ProfileTweakUI.Dialog) {
            tweak.content(
                /* onDismiss = */ { showDialog = false }
            )
        } else {
            TextAlertDialog(
                title = stringResource(id = LocaleR.string.heads_up),
                message = stringResource(id = LocaleR.string.action_warning_format_message, tweak.label.asString()),
                confirmButtonLabel = stringResource(id = LocaleR.string.proceed),
                onConfirm = { tweak.onClick() },
                onDismiss = { showDialog = false }
            )
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                shape = DefaultShape
            )
            .clickable(
                onClick = if (needsDialog) {
                    fun() {
                        showDialog = true
                    }
                } else tweak.onClick
            )
    ) {
        val spacing = getAdaptiveDp(16.dp)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier
                .fillMaxAdaptiveWidth()
                .padding(spacing)
        ) {
            Icon(
                painter = tweak.icon.asPainter(),
                contentDescription = tweak.label.asString(),
                tint = LocalContentColor.current.copy(0.6f),
                modifier = Modifier
                    .size(
                        getAdaptiveDp(
                            dp = 18.dp,
                            increaseBy = 4.dp
                        ),
                    )
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = tweak.label.asString(),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
                )

                Text(
                    text = tweak.description.asString(),
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.bodySmall.asAdaptiveTextStyle(),
                )
            }
        }
    }
}
