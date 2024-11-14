package com.flixclusive.feature.mobile.preferences.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.preferences.UserScreenHorizontalPadding
import com.flixclusive.model.database.User
import kotlin.math.absoluteValue

private val ProfileCircleSize = 100.dp
private val ThreePagesPerViewport = object : PageSize {
    override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int {
        return (availableSpace - 2 * pageSpacing) / 3
    }
}

@Composable
internal fun UsersPager(
    currentUser: User,
    users: List<User>,
    onUserChange: (Int) -> Unit,
) {
    val pagerState = rememberPagerState(
        pageCount = { users.size }
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = UserScreenHorizontalPadding),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = currentUser.name,
                style = MaterialTheme.typography.headlineMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }

        HorizontalPager(
            state = pagerState,
            pageSize = ThreePagesPerViewport,
            contentPadding = PaddingValues(start = UserScreenHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) { page ->
            UserProfileBlock(
                user = users[page],
                modifier = Modifier
                    .graphicsLayer {
                        val pageOffset = (
                                (pagerState.currentPage - page) + pagerState
                                    .currentPageOffsetFraction
                                ).absoluteValue

                        alpha = lerp(
                            start = 0.6f,
                            stop = 1f,
                            fraction = 1f - pageOffset
                        )

                        val scaleFloat = lerp(
                            start = 0.85f,
                            stop = 1f,
                            fraction = 1f - pageOffset
                        )

                        scaleX = scaleFloat
                        scaleY = scaleFloat
                    }
            )
        }
    }

}

@Composable
private fun UserProfileBlock(
    modifier: Modifier = Modifier,
    user: User
) {
    val userImage = remember(user.image) {
        // TODO: Get proper profile image
        -1
    }

    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                shape = CircleShape
            )
            .size(ProfileCircleSize)
    ) {
        // TODO: Display user image
    }
}

@Preview
@Composable
private fun UsersPagerPreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            UsersPager(
                currentUser = User(),
                users = List(5) { User() },
                onUserChange = {}
            )
        }
    }
}