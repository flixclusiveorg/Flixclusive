package com.flixclusive.feature.mobile.player.controls.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.util.onMediumEmphasis


@Composable
internal fun PlayerDialogButton(
    modifier: Modifier = Modifier,
    label: String,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = Color.White.copy(0.1F),
        contentColor = LocalContentColor.current.copy(0.6f)
    ),
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .padding(5.dp)
    ) {
        Button(
            onClick = onClick,
            colors = colors,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .heightIn(min = 50.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
