package com.flixclusive.feature.mobile.provider.test.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.COMMON_TOP_BAR_HEIGHT
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun ProviderTestScreenTopBar(
    modifier: Modifier = Modifier,
    onNavigationIconClick: () -> Unit,
    onOpenSortBottomSheet: () -> Unit
) {
    Box(
        modifier = modifier
            .statusBarsPadding()
            .height(COMMON_TOP_BAR_HEIGHT),
        contentAlignment = Alignment.TopCenter
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigationIconClick) {
                Icon(
                    painter = painterResource(UiCommonR.drawable.left_arrow),
                    contentDescription = stringResource(LocaleR.string.navigate_up)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onOpenSortBottomSheet) {
                Icon(
                    painter = painterResource(UiCommonR.drawable.filter_list),
                    contentDescription = stringResource(LocaleR.string.settings)
                )
            }
        }
    }
}
