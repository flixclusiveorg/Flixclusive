package com.flixclusive.feature.mobile.about.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun Header(
    appName: String,
    versionName: String,
    commitVersion: String,
    isInDebugMode: Boolean,
    isOnPreRelease: Boolean,
) {
    val appNameUppercase = remember { appName.uppercase() }
    val version = versionName + (if (isOnPreRelease) "-[$commitVersion]" else "")
    val mode = when {
        isInDebugMode -> stringResource(id =  LocaleR.string.debug)
        isOnPreRelease -> stringResource(id =  LocaleR.string.pre_release)
        else -> stringResource(id =  LocaleR.string.release)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(
            space = 12.dp,
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 25.dp)
    ) {
        Image(
            painter = painterResource(id = UiCommonR.mipmap.ic_launcher_foreground),
            contentDescription = stringResource(LocaleR.string.flixclusive_icon_content_desc),
            modifier = Modifier
                .scale(2F)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(
                space = 6.dp,
                alignment = Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = appNameUppercase,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 16.sp),
            )

            Surface(
                contentColor = LocalContentColor.current.onMediumEmphasis()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 10.dp,
                        alignment = Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = version,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 13.sp
                        ),
                    )

                    Spacer(
                        modifier = Modifier
                            .height(2.dp)
                            .width(10.dp)
                            .background(
                                LocalContentColor.current,
                                MaterialTheme.shapes.large,
                            )
                    )

                    Text(
                        text = mode,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 13.sp
                        ),
                    )
                }
            }
        }
    }
}