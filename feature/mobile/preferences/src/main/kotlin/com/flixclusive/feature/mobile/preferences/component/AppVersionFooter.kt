package com.flixclusive.feature.mobile.preferences.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun AppVersionFooter(
    modifier: Modifier = Modifier,
    versionName: String,
    commitVersion: String,
    isInDebugMode: Boolean,
    isOnPreRelease: Boolean,
) {
    val context = LocalContext.current
    val version = remember {
        versionName + (if (isOnPreRelease) "-[$commitVersion]" else "")
    }
    val mode = remember {
        when {
            isInDebugMode -> context.getString(LocaleR.string.debug)
            isOnPreRelease -> context.getString(LocaleR.string.pre_release)
            else -> context.getString(LocaleR.string.release)
        }
    }

    val defaultStyle = MaterialTheme.typography.headlineSmall
        .copy(
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = LocalContentColor.current.onMediumEmphasis()
        )

    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(defaultStyle.toSpanStyle()) {
                    append(version)
                    append(" — ")
                    append(mode)
                }
            },
        )
    }
}

@Preview
@Composable
private fun AppVersionFooterOnDebugPreview() {
    FlixclusiveTheme {
        Surface {
            AppVersionFooter(
                versionName = "1.0.0",
                commitVersion = "a1e62eq",
                isInDebugMode = true,
                isOnPreRelease = false
            )
        }
    }
}

@Preview
@Composable
private fun AppVersionFooterOnReleasePreview() {
    FlixclusiveTheme {
        Surface {
            AppVersionFooter(
                versionName = "1.2.4",
                commitVersion = "1ae26av",
                isInDebugMode = false,
                isOnPreRelease = false
            )
        }
    }
}

@Preview
@Composable
private fun AppVersionFooterOnPreReleasePreview() {
    FlixclusiveTheme {
        Surface {
            AppVersionFooter(
                versionName = "1.0.0",
                commitVersion = "ks2m00x",
                isInDebugMode = false,
                isOnPreRelease = true
            )
        }
    }
}