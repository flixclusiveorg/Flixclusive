package com.flixclusive.core.ui.common.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.R
import com.flixclusive.core.locale.R as LocaleR

/**
 *
 * A custom icon labeled alert dialog
 * */
@Composable
fun IconAlertDialog(
    painter: Painter,
    contentDescription: String?,
    description: CharSequence,
    tint: Color = MaterialTheme.colorScheme.primary,
    dialogProperties: DialogProperties = DialogProperties(),
    confirmButtonLabel: String = stringResource(id = LocaleR.string.ok),
    dismissButtonLabel: String? = stringResource(id = LocaleR.string.cancel),
    dismissOnConfirm: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val buttonMinHeight = 50.dp
    val cornerSize = CornerSize(
        (ALERT_DIALOG_ROUNDNESS_PERCENTAGE * 2).dp
    )
    val buttonShape = MaterialTheme.shapes.medium.let {
        val bottomEnd = when (dismissButtonLabel) {
            null -> cornerSize
            else -> it.bottomEnd
        }

        it.copy(
            bottomStart = cornerSize,
            bottomEnd = bottomEnd,
        )
    }

    CustomBaseAlertDialog(
        onDismiss = onDismiss,
        dialogProperties = dialogProperties,
        action = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 10.dp)
            ) {
                Button(
                    onClick = {
                        onConfirm()

                        if (dismissOnConfirm)
                            onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    ),
                    shape = buttonShape,
                    modifier = Modifier
                        .weight(1F)
                        .heightIn(min = buttonMinHeight)
                ) {
                    Text(
                        text = confirmButtonLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(end = 2.dp)
                    )
                }

                if (dismissButtonLabel != null) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = buttonShape.copy(
                            bottomEnd = cornerSize,
                        ),
                        modifier = Modifier
                            .weight(1F)
                            .heightIn(min = buttonMinHeight)
                    ) {
                        Text(
                            text = dismissButtonLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
        }
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .padding(10.dp)
        )

        CharSequenceText(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .padding(horizontal = 10.dp)
                .verticalScroll(rememberScrollState())
        )
    }
}


@Preview(device = "spec:width=1344px,height=2992px,dpi=640")
@Composable
private fun CommonNoticeDialogPreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            IconAlertDialog(
                painter = painterResource(id = R.drawable.warning),
                contentDescription = stringResource(
                    id = LocaleR.string.warning_content_description
                ),
                dismissButtonLabel = null,
                description = " Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus diam orci, blandit sit amet dolor nec, congue ultricies risus. Etiam porttitor bibendum elit, vitae luctus libero feugiat quis. Praesent vel finibus nisl. Ut quis libero mi. Proin molestie eros elit, in condimentum justo aliquam ut. Suspendisse gravida luctus ornare. Morbi nulla est, pretium a vestibulum nec, lobortis ac mauris. Sed viverra ipsum quis scelerisque cursus. Praesent sed libero enim. Aenean eleifend ut urna in commodo. Pellentesque pretium tempor magna, nec tempor arcu venenatis vitae. Aliquam lacinia faucibus leo, facilisis viverra sapien ullamcorper eu.\n" +
                        "\n" +
                        "Integer ullamcorper libero vel efficitur vulputate. Sed aliquam mauris eu sollicitudin fringilla. Proin accumsan, sem non pulvinar tristique, lacus turpis vehicula ante, id feugiat nisl risus in ex. Aenean lacus sem, dapibus eget vestibulum sed, condimentum eu nulla. Praesent fringilla eros vel libero fermentum, non varius quam pharetra. Mauris nec ligula nibh. Pellentesque vel orci ut libero rutrum tempor. Maecenas fermentum lorem enim, quis commodo ante bibendum et. Integer aliquam mi id magna porta, ut viverra lacus ultrices. Nunc aliquet ut ante at ornare. Proin placerat egestas est, sed suscipit metus porta malesuada. Sed ligula lectus, iaculis nec lacinia in, commodo sed orci. Integer maximus, justo ut rutrum dignissim, ex eros maximus lacus, ut finibus massa neque vel velit. Proin sodales turpis ac ipsum lacinia volutpat.\n" +
                        "\n" +
                        "Duis volutpat diam a odio cursus iaculis. Ut consequat, ipsum quis scelerisque efficitur, ligula enim facilisis est, quis ornare turpis orci eget risus. Vestibulum diam lorem, bibendum eget magna ac, iaculis aliquam nisl. In pulvinar malesuada quam, non viverra leo auctor sit amet. Vivamus quis neque nulla. Duis ante ligula, dignissim in justo in, tempus venenatis odio. Morbi rhoncus diam ac luctus placerat. Ut scelerisque tempor sem, et condimentum eros volutpat a. Donec euismod lobortis ligula nec ornare. Phasellus no",
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(device = "spec:width=1344px,height=2992px,dpi=640")
@Composable
private fun CommonNoticeDialogPreviewPart2() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            IconAlertDialog(
                painter = painterResource(id = R.drawable.warning),
                contentDescription = stringResource(id = LocaleR.string.warning_content_description),
                description = " Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus diam orci, blandit sit amet dolor nec, congue ultricies risus. Etiam porttitor bibendum",
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}