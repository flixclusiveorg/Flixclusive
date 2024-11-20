package com.flixclusive.feature.mobile.preferences.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.user.UserAvatar
import com.flixclusive.feature.mobile.preferences.util.UiUtil.getEmphasizedLabel
import com.flixclusive.model.database.User

@Composable
internal fun AvatarBlock(
    modifier: Modifier = Modifier,
    currentUser: User,
    onChangeUser: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .statusBarsPadding()
    ) {
        UserAvatar(user = currentUser)

        Box(
            modifier = Modifier
                .fillMaxWidth(0.4F),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentUser.name,
                style = getEmphasizedLabel(16.sp),
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }

}

@Preview
@Composable
private fun UsersPagerPreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxHeight(0.5F)
                .fillMaxWidth()
        ) {
            Box(contentAlignment = Alignment.Center) {
                AvatarBlock(
                    currentUser = User(name = "John Doe"),
                    onChangeUser = {}
                )
            }
        }
    }
}