package com.flixclusive.core.ui.common.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

const val ALERT_DIALOG_CORNER_SIZE = 10

// TODO: Fix CornerSize UI issue

/**
 *
 * A custom alert dialog that can be used to show a message to the user.
 * */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomBaseAlertDialog(
    onDismiss: () -> Unit,
    dialogProperties: DialogProperties = DialogProperties(),
    action: @Composable BoxScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = dialogProperties,
    ) {
        Surface(
            shape = RoundedCornerShape(ALERT_DIALOG_CORNER_SIZE),
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp)
            ) {
                Column(
                    content = content,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(10.dp)
                        .weight(1F, fill = false)
                )

                Box(content = action)
            }
        }
    }
}

@Composable
fun CharSequenceText(
    modifier: Modifier = Modifier,
    text: CharSequence,
    style: TextStyle,
) {
    when (text) {
        is String -> Text(
            text = text,
            style = style,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = modifier
        )
        is AnnotatedString -> Text(
            text = text,
            style = style,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = modifier
        )
    }
}