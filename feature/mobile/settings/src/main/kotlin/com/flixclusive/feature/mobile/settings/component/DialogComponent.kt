package com.flixclusive.feature.mobile.settings.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.dialog.TextAlertDialog
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
internal fun DialogComponent(
    title: String,
    dialogTitle: String,
    dialogMessage: String,
    description: String? = null,
    icon: Painter? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
) {
    var isDialogShown by rememberSaveable { mutableStateOf(false) }

    val onDismissRequest = fun () { isDialogShown = false }

    ClickableComponent(
        modifier = modifier,
        title = title,
        description = description,
        enabled = enabled,
        icon = icon,
        onClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        TextAlertDialog(
            label = dialogTitle,
            description = dialogMessage,
            onDismiss = onDismissRequest,
            onConfirm = onConfirm,
        )
    }
}

@Preview
@Composable
private fun DialogComponentBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Column {
                DialogComponent(
                    title = "Dialog tweak with icon",
                    description = "Dialog tweak summary",
                    dialogTitle = "Dialog tweak",
                    dialogMessage = "Dialog tweak summary",
                    icon = painterResource(UiCommonR.drawable.happy_emphasized),
                    onConfirm = {},
                )

                DialogComponent(
                    title = "Dialog tweak",
                    description = "Dialog tweak summary",
                    dialogTitle = "Dialog tweak",
                    dialogMessage = "Dialog tweak summary",
                    onConfirm = {},
                )

                DialogComponent(
                    title = "Dialog tweak no summary",
                    dialogTitle = "Dialog no summary",
                    dialogMessage = "Dialog tweak no summary",
                    onConfirm = {},
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun DialogComponentCompactLandscapePreview() {
    DialogComponentBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun DialogComponentMediumPortraitPreview() {
    DialogComponentBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun DialogComponentMediumLandscapePreview() {
    DialogComponentBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun DialogComponentExtendedPortraitPreview() {
    DialogComponentBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun DialogComponentExtendedLandscapePreview() {
    DialogComponentBasePreview()
}