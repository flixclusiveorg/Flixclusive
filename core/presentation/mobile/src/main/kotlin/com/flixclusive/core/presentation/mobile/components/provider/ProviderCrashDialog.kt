package com.flixclusive.core.presentation.mobile.components.provider

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.R
import com.flixclusive.core.presentation.mobile.components.material3.dialog.CommonAlertDialog
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.theme.MobileColors.surfaceColorAtElevation
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun ProviderCrashDialog(
    provider: ProviderMetadata,
    error: Throwable,
    onDismissRequest: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    val stackTrace = remember { error.stackTraceToString() }

    CommonAlertDialog(
        onDismiss = onDismissRequest,
        shape = MaterialTheme.shapes.small,
        action = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CommonButton(
                    onClick = { uriHandler.openUri(provider.repositoryUrl + "/issues/new") },
                    label = stringResource(R.string.report),
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                )

                CommonButton(
                    onClick = onDismissRequest,
                    label = stringResource(LocaleR.string.cancel),
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3),
                    contentColor = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                )
            }
        },
    ) {
        CrashItemTopContent(
            provider = provider,
            modifier = Modifier.fillMaxWidth(),
        )

        ScrollableStackTrace(
            error = stackTrace,
            modifier = Modifier.padding(vertical = 10.dp),
        )
    }
}

@Composable
private fun ScrollableStackTrace(
    error: String,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .height(200.dp)
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = LocalContentColor.current.copy(0.6f),
                shape = MaterialTheme.shapes.extraSmall,
            )
            .background(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1),
                shape = MaterialTheme.shapes.extraSmall,
            ),
    ) {
        item {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall
                    .copy(fontFamily = FontFamily.Monospace)
                    .asAdaptiveTextStyle(10.sp),
                softWrap = false,
                color = LocalContentColor.current.copy(0.8f),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .padding(4.dp)
                    .horizontalScroll(rememberScrollState()),
            )
        }
    }
}

@Composable
private fun RowScope.CommonButton(
    onClick: () -> Unit,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .weight(1F)
            .heightIn(min = 40.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle(),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview
@Composable
private fun ProviderCrashDialogPreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            ProviderCrashDialog(
                provider = DummyDataForPreview.getDummyProviderMetadata(),
                error = NullPointerException("This is a sample error message for provider."),
                onDismissRequest = {},
            )
        }
    }
}
