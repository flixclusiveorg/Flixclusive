package com.flixclusive.feature.mobile.profiles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.user.UserAvatar
import com.flixclusive.core.ui.common.user.UserAvatarDefaults.AVATARS_IMAGE_COUNT
import com.flixclusive.core.ui.common.user.UserAvatarDefaults.DefaultAvatarShape
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveSemiEmphasizedLabel
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveTextUnit
import com.flixclusive.core.ui.common.util.animation.AnimationUtil.getLocalAnimatedVisibilityScope
import com.flixclusive.core.ui.common.util.animation.AnimationUtil.getLocalSharedTransitionScope
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.profiles.component.EditButton
import com.flixclusive.model.database.User
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

private val DefaultAvatarGridSize = 90.dp

private val CompactLabelSize = 12.sp

@Composable
internal fun GridMode(
    modifier: Modifier = Modifier,
    listState: LazyGridState,
    profiles: List<User>,
    onSelect: (User) -> Unit,
    onEdit: (User) -> Unit
) {
    val widthFraction = 0.8F
    val lazyGridSizeModifier = Modifier
        .fillMaxWidth(widthFraction)
        .fillMaxHeight(0.65F)
    val columnsSize = getAdaptiveDp(
        dp = DefaultAvatarGridSize,
        incrementedDp = 70.dp
    )

    val surfaceColor = MaterialTheme.colorScheme.surface
    val scrimOverlayHeight = 50.dp
    val scrimOverlayAdaptiveHeight = getAdaptiveDp(scrimOverlayHeight)

    var isEditing by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(
            space = 25.dp,
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(columnsSize),
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(vertical = scrimOverlayAdaptiveHeight),
                modifier = Modifier
                    .align(Alignment.Center)
                    .then(lazyGridSizeModifier)
            ) {
                items(profiles) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        UserAvatarWithEdit(
                            modifier = Modifier.animateItem(),
                            user = it,
                            isEditing = isEditing,
                            onSelect = {
                                if (isEditing) {
                                    onEdit(it)
                                } else {
                                    onSelect(it)
                                }
                            },
                        )

                        UsernameTag(user = it)
                    }
                }
            }

            Box(
                modifier = lazyGridSizeModifier
                    .align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(scrimOverlayAdaptiveHeight)
                        .align(Alignment.TopCenter)
                        .drawWithContent {
                            drawRect(
                                Brush.verticalGradient(
                                    0F to surfaceColor,
                                    1F to Color.Transparent,
                                )
                            )
                            drawContent()
                        }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(scrimOverlayAdaptiveHeight)
                        .align(Alignment.BottomCenter)
                        .drawWithContent {
                            drawRect(
                                Brush.verticalGradient(
                                    0F to Color.Transparent,
                                    1F to surfaceColor,
                                )
                            )
                            drawContent()
                        }
                )
            }
        }

        EditButton(
            isEditing = isEditing,
            onEdit = { isEditing = !isEditing },
            buttonSize = getAdaptiveDp(
                compact = 25.dp,
                medium = 40.dp,
                expanded = 60.dp
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
                horizontal = getAdaptiveDp(5.dp, 9.5.dp)
            )
        )
    }
}

@Composable
private fun UsernameTag(
    modifier: Modifier = Modifier,
    user: User
) {
    val columnsSize = getAdaptiveDp(
        dp = DefaultAvatarGridSize,
        incrementedDp = 70.dp
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(
            space = 3.dp,
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = user.name,
            style = getAdaptiveSemiEmphasizedLabel(
                compact = CompactLabelSize,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = modifier
                .width(columnsSize)
        )

        // TODO: Add conditional statement if User is locked
        if (true) {
            Icon(
                painter = painterResource(UiCommonR.drawable.lock_thin),
                contentDescription = stringResource(LocaleR.string.locked_profile_button_desc),
                tint = LocalContentColor.current.onMediumEmphasis(0.8F),
                modifier = Modifier.size(
                    getAdaptiveDp(dp = 16.dp)
                )
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun UserAvatarWithEdit(
    modifier: Modifier = Modifier,
    user: User,
    isEditing: Boolean,
    onSelect: (User) -> Unit,
) {
    val sharedTransitionScope = getLocalSharedTransitionScope()
    val animatedVisibilityScope = getLocalAnimatedVisibilityScope()

    val fadeDuration = 200
    val avatarModifier = Modifier
        .aspectRatio(1F)
        .clickable { onSelect(user) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        with(sharedTransitionScope) {
            UserAvatar(
                user = user,
                shadowBlur = 30.dp,
                modifier = avatarModifier
                    .sharedElement(
                        state = rememberSharedContentState(key = "${user.id}-grid"),
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
            )
        }

        AnimatedVisibility(
            visible = isEditing,
            enter = fadeIn(tween(fadeDuration)) + scaleIn(tween(350)),
            exit = scaleOut(tween(400)) + fadeOut(tween(fadeDuration))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = avatarModifier
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8F),
                        shape = DefaultAvatarShape
                    )
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.onMediumEmphasis(0.4f),
                        shape = DefaultAvatarShape
                    )
            ) {
                Icon(
                    painter = painterResource(UiCommonR.drawable.edit),
                    contentDescription = stringResource(LocaleR.string.edit_profile_button_desc),
                    modifier = Modifier.size(
                        getAdaptiveDp(30.dp, 30.dp)
                    )
                )
            }
        }
    }
}

@Preview
@Composable
private fun GridModeBasePreview() {
    FlixclusiveTheme {
        Surface {
            GridMode(
                onEdit = {},
                onSelect = {},
                listState = rememberLazyGridState(),
                profiles = List(20) {
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
private fun GridModeCompactLandscapePreview() {
    GridModeBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun GridModeMediumPortraitPreview() {
    GridModeBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun GridModeMediumLandscapePreview() {
    GridModeBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun GridModeExtendedPortraitPreview() {
    GridModeBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun GridModeExtendedLandscapePreview() {
    GridModeBasePreview()
}