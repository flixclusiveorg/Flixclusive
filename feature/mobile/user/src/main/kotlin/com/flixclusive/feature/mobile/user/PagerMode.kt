package com.flixclusive.feature.mobile.user

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.user.AVATARS_IMAGE_COUNT
import com.flixclusive.core.ui.common.user.DefaultAvatarSize
import com.flixclusive.core.ui.common.user.UserAvatar
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveNonEmphasizedLabel
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveTextUnit
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.user.component.EditButton
import com.flixclusive.feature.mobile.user.util.ModifierUtil.getPagerBlur
import com.flixclusive.feature.mobile.user.util.ModifierUtil.scaleDownOnPress
import com.flixclusive.model.database.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

private val PagerAvatarSize = DefaultAvatarSize * 2

@Composable
internal fun PagerMode(
    modifier: Modifier = Modifier,
    profiles: List<User>,
    onSelect: (User) -> Unit,
    onEdit: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val indexPressed = remember { mutableStateOf<Int?>(null) }
    val pageCount = if (profiles.size <= 2) {
        profiles.size
    } else Int.MAX_VALUE

    val initialPage =
        if (profiles.size <= 2) 0
        else (Int.MAX_VALUE / 2) - 3

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val pageWidth = (screenWidthDp / 3f).dp
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { pageCount }
    )

    var showEditButton by remember { mutableStateOf(true) }

    /*
    * UX HACK FOR EDIT BUTTON VISIBILITY
    * */
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && !showEditButton) {
            delay(800)
            showEditButton = true
        } else if (pagerState.isScrollInProgress && showEditButton) {
            showEditButton = false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(25.dp, Alignment.CenterVertically),
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(visible = showEditButton) {
                EditButton(
                    onEdit = onEdit,
                    buttonSize = getAdaptiveDp(
                        compact = 25.dp,
                        medium = 35.dp,
                        expanded = 45.dp
                    ),
                    iconSize = getAdaptiveDp(
                        dp = 14.dp,
                        incrementedDp = 6.dp
                    ),
                    spacing = getAdaptiveDp(5.dp, 2.dp),
                    fontSize = getAdaptiveTextUnit(
                        size = 12.sp,
                        incrementedValue = 2
                    ),
                    contentPadding = PaddingValues(
                        horizontal = getAdaptiveDp(5.dp, 6.dp)
                    )
                )
            }

            HorizontalPager(
                modifier = Modifier.height(pageWidth),
                contentPadding = PaddingValues(horizontal = pageWidth),
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
                        val scaleValueOnPress by animateFloatAsState(
                            label = "scale_value",
                            targetValue = if (indexPressed.value == page) 0.95F else 1F,
                            animationSpec = tween(durationMillis = 300)
                        )

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
                                .size(pageWidth)
                                .scaleDownOnPress(
                                    index = page,
                                    pressState = indexPressed
                                ) {
                                    if (pagerState.currentPage == page) {
                                        onSelect(item)
                                    } else {
                                        scope.launch {
                                            pagerState.animateScrollToPage(
                                                page = page,
                                                animationSpec = tween(durationMillis = 500)
                                            )
                                            onSelect(item)
                                        }
                                    }
                                }
                                .graphicsLayer {
                                    scaleX = scaleValueOnPress
                                    scaleY = scaleValueOnPress
                                }
                                .graphicsLayer {
                                    this.clip = true
                                    this.shape = blurShape
                                    this.renderEffect = getPagerBlur(pageOffset = pageOffset)

                                    val scale = lerp(
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
    modifier: Modifier = Modifier,
    profiles: List<User>,
    currentPage: () -> Int,
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
                SizeTransform(clip = false)
            )
        },
        label = "username_animation"
    ) { page ->
        val item = remember {
            profiles.getOrNull(page % profiles.size)
                ?: throw NullPointerException("Scrolled user [$page] is null")
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(
                space = 5.dp,
                alignment = Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .width(PagerAvatarSize)
        ) {
            Text(
                text = item.name,
                style = getAdaptiveNonEmphasizedLabel(18.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // TODO: Add conditional statement if User is locked
            if (true) {
                Icon(
                    painter = painterResource(UiCommonR.drawable.lock_thin),
                    contentDescription = stringResource(LocaleR.string.locked_profile_button_desc),
                    tint = LocalContentColor.current.onMediumEmphasis(0.8F),
                    modifier = Modifier.size(getAdaptiveDp(16.dp, 6.dp))
                )
            }
        }
    }
}

@Preview(device = "id:pixel_9_pro")
@Composable
private fun PagerModeBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            PagerMode(
                onEdit = {},
                onSelect = {},
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