package com.flixclusive.feature.mobile.preferences.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun PreferencesItem(
    @DrawableRes iconId: Int,
    @StringRes labelId: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(35.dp)
                .padding(start = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconId),
                contentDescription = stringResource(id = labelId)
            )
        }

        Text(
            text = stringResource(id = labelId),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            ),
            modifier = Modifier
                .padding(start = 12.dp)
        )
    }
}
