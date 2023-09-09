package com.flixclusive.presentation.mobile.screens.preferences.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.BuildConfig
import com.flixclusive.R
import com.flixclusive.presentation.mobile.screens.preferences.PreferencesNavGraph
import com.flixclusive.presentation.mobile.screens.preferences.common.TopBarWithNavigationIcon
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils
import com.flixclusive.presentation.utils.FormatterUtils.toTitleCase
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@PreferencesNavGraph
@Destination
@Composable
fun AboutMobileScreen(
    navigator: DestinationsNavigator
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize(),
    ) {
        TopBarWithNavigationIcon(
            headerTitle = stringResource(id = R.string.about),
            onNavigationIconClick = navigator::navigateUp
        )

        AboutScreenHeader()

    }
}

@Composable
private fun AboutScreenHeader() {
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
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = "Flixclusive Icon",
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
                text = stringResource(id = R.string.app_name).uppercase(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 16.sp,
                    //letterSpacing = TextUnit(1.05F, TextUnitType.Sp)
                ),
            )

            Surface(
                contentColor = ComposeMobileUtils.colorOnMediumEmphasisMobile()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 10.dp,
                        alignment = Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = BuildConfig.VERSION_NAME,
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
                        text = BuildConfig.BUILD_TYPE.toTitleCase(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 13.sp
                        ),
                    )
                }
            }
        }
    }
}