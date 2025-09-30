package com.flixclusive.feature.mobile.profiles

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.presentation.common.util.SharedTransitionUtil.ProvideAnimatedVisibilityScope
import com.flixclusive.core.presentation.common.util.SharedTransitionUtil.ProvideSharedTransitionScope
import com.flixclusive.core.presentation.common.util.SharedTransitionUtil.getLocalAnimatedVisibilityScope
import com.flixclusive.core.presentation.common.util.SharedTransitionUtil.getLocalSharedTransitionScope
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.UserAvatar
import com.flixclusive.core.presentation.mobile.components.UserAvatarDefaults.AVATARS_IMAGE_COUNT
import com.flixclusive.core.presentation.mobile.components.UserAvatarDefaults.DefaultAvatarSize
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveTextUnit
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.feature.mobile.profiles.component.EditButton
import com.flixclusive.feature.mobile.profiles.util.ModifierUtil.getPagerBlur
import com.flixclusive.feature.mobile.profiles.util.ModifierUtil.scaleDownOnPress
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

private val PagerAvatarSize = DefaultAvatarSize * 2

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PagerMode(
    pagerState: PagerState,
    profiles: List<User>,
    onHover: (User) -> Unit,
    onEdit: (User) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val sharedTransitionScope = getLocalSharedTransitionScope()
    val animatedVisibilityScope = getLocalAnimatedVisibilityScope()

    val indexPressed = remember { mutableStateOf<Int?>(null) }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val widthPercentage = if (isLandscape) 0.5F else 1F
    val pageWidth = ((screenWidthDp * widthPercentage) / 3).dp
    val horizontalPadding = ((screenWidthDp / 2)).dp - (pageWidth / 1.9f)

    val enableEditButton by remember {
        derivedStateOf {
            !pagerState.isScrollInProgress
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(25.dp, Alignment.CenterVertically),
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EditButton(
                onEdit = {
                    val selected = profiles.getOrNull(
                        index = pagerState.currentPage % profiles.size,
                    ) ?: return@EditButton

                    onEdit(selected)
                },
                enabled = enableEditButton,
                buttonSize = getAdaptiveDp(
                    compact = 25.dp,
                    medium = 35.dp,
                    expanded = 45.dp,
                ),
                iconSize = getAdaptiveDp(
                    dp = 14.dp,
                    increaseBy = 6.dp,
                ),
                spacing = getAdaptiveDp(5.dp, 2.dp),
                fontSize = getAdaptiveTextUnit(size = 12.sp, increaseBy = 2),
                contentPadding = PaddingValues(
                    horizontal = getAdaptiveDp(5.dp, 6.dp),
                ),
            )

            HorizontalPager(
                modifier = Modifier.height(pageWidth),
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                state = pagerState,
            ) { page ->
                profiles
                    .getOrNull(
                        index = page % profiles.size,
                    )?.let { item ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth(),
                        ) {
                            val blurShape = MaterialTheme.shapes.small
                            val scaleValueOnPress by animateFloatAsState(
                                label = "scale_value",
                                targetValue = if (indexPressed.value == page) 0.95F else 1F,
                                animationSpec = tween(durationMillis = 300),
                            )

                            with(sharedTransitionScope) {
                                UserAvatar(
                                    avatar = item.image,
                                    shadowBlur = 30.dp,
                                    modifier = Modifier
                                        .sharedElement(
                                            state = rememberSharedContentState(key = "${item.id}-pager"),
                                            animatedVisibilityScope = animatedVisibilityScope,
                                        ).size(pageWidth)
                                        .scaleDownOnPress(
                                            index = page,
                                            pressState = indexPressed,
                                        ) {
                                            if (pagerState.currentPage == page) {
                                                onHover(item)
                                            } else {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(
                                                        page = page,
                                                        animationSpec = tween(durationMillis = 500),
                                                    )
                                                    onHover(item)
                                                }
                                            }
                                        }.graphicsLayer {
                                            scaleX = scaleValueOnPress
                                            scaleY = scaleValueOnPress
                                        }.graphicsLayer {
                                            val pageOffset = (
                                                (pagerState.currentPage - page) +
                                                    pagerState.currentPageOffsetFraction
                                                ).absoluteValue

                                            this.clip = true
                                            this.shape = blurShape
                                            this.renderEffect = getPagerBlur(pageOffset = pageOffset)

                                            val scale = lerp(
                                                start = 0.7f,
                                                stop = 1f,
                                                fraction = 1f - pageOffset,
                                            )

                                            this.scaleX = scale
                                            this.scaleY = scale

                                            this.alpha = lerp(
                                                start = 0.2f,
                                                stop = 1f,
                                                fraction = 1f - pageOffset,
                                            )
                                        },
                                )
                            }
                        }
                    }
            }
        }

        val currentPage by remember {
            derivedStateOf {
                pagerState.currentPage
            }
        }

        UsernameTag(
            profiles = profiles,
            currentPage = { currentPage },
        )
    }
}

@Composable
private fun UsernameTag(
    profiles: List<User>,
    currentPage: () -> Int,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = currentPage(),
        transitionSpec = {
            val tweenInt = tween<IntOffset>(durationMillis = 300)
            val tweenFloat = tween<Float>(durationMillis = 500)
            val widthDivisor = 6

            if (targetState > initialState) {
                fadeIn(tweenFloat) + slideInHorizontally(animationSpec = tweenInt) { it / widthDivisor } togetherWith
                    fadeOut() + slideOutHorizontally { -it / widthDivisor }
            } else {
                fadeIn(tweenFloat) + slideInHorizontally(tweenInt) { -it / widthDivisor } + fadeIn() togetherWith
                    fadeOut() + slideOutHorizontally { it / widthDivisor }
            }.using(
                SizeTransform(clip = false),
            )
        },
        label = "username_animation",
    ) { page ->
        val item = profiles.getOrNull(page % profiles.size)
            ?: throw NullPointerException("Scrolled user [$page] is null")

        Column(
            verticalArrangement = Arrangement.spacedBy(
                space = 5.dp,
                alignment = Alignment.CenterVertically,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .width(PagerAvatarSize),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(
                    compact = 18.sp,
                    expanded = 30.sp,
                ),
                fontWeight = FontWeight.Normal,
                color = LocalContentColor.current.copy(0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (item.pin != null) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.lock_thin),
                    contentDescription = stringResource(LocaleR.string.locked_profile_button_desc),
                    tint = LocalContentColor.current.copy(0.8F),
                    dp = 16.dp,
                    increaseBy = 8.dp,
                )
            } else {
                Spacer(modifier = Modifier.size(getAdaptiveDp(16.dp, 8.dp)))
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(device = "id:pixel_9_pro")
@Composable
private fun PagerModeBasePreview() {
    val profiles = List(10) {
        User(
            id = it,
            image = it % AVATARS_IMAGE_COUNT,
            name = "User $it",
        )
    }
    val pageCount = if (profiles.size <= 2) {
        profiles.size
    } else {
        Int.MAX_VALUE
    }

    val initialPage =
        if (profiles.size <= 2) {
            0
        } else {
            (Int.MAX_VALUE / 2) - 3
        }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { pageCount },
    )

    ProvideSharedTransitionScope {
        FlixclusiveTheme {
            Surface(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                AnimatedVisibility(true) {
                    ProvideAnimatedVisibilityScope {
                        PagerMode(
                            onEdit = {},
                            onHover = {},
                            profiles = profiles,
                            pagerState = pagerState,
                        )
                    }
                }
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun PagerModeCompactLandscapePreview() {
    PagerModeBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun PagerModeMediumPortraitPreview() {
    PagerModeBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun PagerModeMediumLandscapePreview() {
    PagerModeBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun PagerModeExtendedPortraitPreview() {
    PagerModeBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun PagerModeExtendedLandscapePreview() {
    PagerModeBasePreview()
}
