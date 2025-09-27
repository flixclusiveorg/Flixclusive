package com.flixclusive.feature.mobile.provider.test.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.topbar.CommonTopBarDefaults.DefaultTopBarHeight
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun ProviderTestScreenTopBar(
    onNavigationIconClick: () -> Unit,
    onOpenSortBottomSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .statusBarsPadding()
            .height(DefaultTopBarHeight),
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
        ) {
            IconButton(onClick = onNavigationIconClick) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.left_arrow),
                    contentDescription = stringResource(LocaleR.string.navigate_up),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onOpenSortBottomSheet) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.filter_list),
                    contentDescription = stringResource(LocaleR.string.settings),
                )
            }
        }
    }
}
