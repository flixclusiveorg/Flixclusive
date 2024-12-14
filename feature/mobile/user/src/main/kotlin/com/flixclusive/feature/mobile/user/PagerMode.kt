package com.flixclusive.feature.mobile.user

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.user.AVATARS_IMAGE_COUNT
import com.flixclusive.core.ui.common.user.DefaultAvatarSize
import com.flixclusive.core.ui.common.user.UserAvatar
import com.flixclusive.feature.mobile.user.util.ModifierUtil.getPagerBlur
import com.flixclusive.feature.mobile.user.util.ModifierUtil.scaleDownOnPress
import com.flixclusive.feature.mobile.user.util.StylesUtil.getNonEmphasizedLabel
import com.flixclusive.model.database.User
import kotlin.math.absoluteValue

@Composable
internal fun PagerMode(
    modifier: Modifier = Modifier,
    profiles: List<User>,
) {
    val indexPressed = remember { mutableStateOf<Int?>(null) }
    val pageCount = if (profiles.size <= 2) {
        profiles.size
    } else Int.MAX_VALUE

    val initialPage =
        if (profiles.size <= 2) 0
        else (Int.MAX_VALUE / 2) - 3

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { pageCount }
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(25.dp, Alignment.CenterVertically),
        modifier = modifier.fillMaxSize()
    ) {
        HorizontalPager(
            modifier = Modifier.height(250.dp),
            pageSpacing = (-(DefaultAvatarSize + 100.dp)),
            state = pagerState
        ) { page ->
            profiles.getOrNull(
                index = page % profiles.size
            )?.let { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    val blurShape = MaterialTheme.shapes.small
                    val pageOffset by remember {
                        derivedStateOf {
                            ((pagerState.currentPage - page) +
                                    pagerState.currentPageOffsetFraction
                                    ).absoluteValue
                        }
                    }

                    UserAvatar(
                        user = item,
                        boxShadowBlur = 30.dp,
                        modifier = Modifier
                            .size(DefaultAvatarSize * 2)
                            .scaleDownOnPress(
                                index = page,
                                pressState = indexPressed
                            )
                            .graphicsLayer {
                                this.clip = true
                                this.shape = blurShape
                                this.renderEffect = getPagerBlur(pageOffset = pageOffset)

                                val scale =
                                    if (indexPressed.value == page) 0.95F
                                    else lerp(
                                        start = 0.7f,
                                        stop = 1f,
                                        fraction = 1f - pageOffset
                                    )

                                this.scaleX = scale
                                this.scaleY = scale

                                this.alpha = lerp(
                                    start = 0.2f,
                                    stop = 1f,
                                    fraction = 1f - pageOffset
                                )
                            }
                    )
                }
            }
        }

        val currentPage by remember {
            derivedStateOf {
                pagerState.currentPage
            }
        }

        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                val tweenInt = tween<IntOffset>(durationMillis = 300)
                val tweenFloat = tween<Float>(durationMillis = 500)

                if (targetState > initialState) {
                    fadeIn(tweenFloat) + slideInHorizontally(animationSpec = tweenInt) { it } togetherWith
                            fadeOut() + slideOutHorizontally { -it / 2 }
                } else {
                    fadeIn(tweenFloat) + slideInHorizontally(tweenInt) { -it } + fadeIn() togetherWith
                            fadeOut() + slideOutHorizontally { it / 2 }
                }.using(
                    SizeTransform(clip = false)
                )
            },
            label = "username_animation"
        ) { page ->
            val item = remember {
                profiles.getOrNull(page % profiles.size)
                    ?: throw NullPointerException("Scrolled user [$page] is null")
            }

            Text(
                text = item.name,
                style = getNonEmphasizedLabel(18.sp)
            )
        }
    }
}


@Preview
@Composable
private fun PagerModePreview() {
    FlixclusiveTheme {
        Surface {
            PagerMode(
                profiles = List(10) {
                    User(
                        id = it,
                        image = it % AVATARS_IMAGE_COUNT,
                        name = "User $it"
                    )
                }
            )
        }
    }
}