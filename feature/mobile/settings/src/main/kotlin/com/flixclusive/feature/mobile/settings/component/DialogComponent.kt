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
import com.flixclusive.core.presentation.mobile.components.material3.dialog.TextAlertDialog
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.drawables.R as UiCommonR

@Composable
internal fun DialogComponent(
    title: String,
    dialogTitle: String,
    dialogMessage: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    enabledProvider: () -> Boolean = { true },
    descriptionProvider: (() -> String)? = null,
    icon: Painter? = null,
) {
    var isDialogShown by rememberSaveable { mutableStateOf(false) }

    val onDismissRequest = fun() {
        isDialogShown = false
    }

    ClickableComponent(
        modifier = modifier,
        title = title,
        descriptionProvider = descriptionProvider,
        enabledProvider = enabledProvider,
        icon = icon,
        onClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        TextAlertDialog(
            title = dialogTitle,
            message = dialogMessage,
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
            modifier = Modifier.fillMaxSize(),
        ) {
            Column {
                DialogComponent(
                    title = "Dialog tweak with icon",
                    descriptionProvider = { "Dialog tweak summary" },
                    dialogTitle = "Dialog tweak",
                    dialogMessage = "Dialog tweak summary",
                    icon = painterResource(UiCommonR.drawable.happy_emphasized),
                    onConfirm = {},
                )

                DialogComponent(
                    title = "Dialog tweak",
                    descriptionProvider = { "Dialog tweak summary" },
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
