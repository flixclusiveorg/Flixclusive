package com.flixclusive.core.presentation.mobile.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import com.flixclusive.core.presentation.mobile.components.UserAvatarDefaults.DefaultAvatarShape
import com.flixclusive.core.presentation.mobile.components.UserAvatarDefaults.DefaultAvatarSize
import com.flixclusive.core.presentation.mobile.extensions.boxShadow
import com.flixclusive.core.presentation.mobile.extensions.getAvatarResource
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import kotlin.random.Random
import com.flixclusive.core.strings.R as LocaleR

object UserAvatarDefaults {
    val DefaultAvatarShape = RoundedCornerShape(8.0.dp)
    val DefaultAvatarSize = 100.dp
    const val AVATAR_PREFIX = "avatar"
    const val AVATARS_IMAGE_COUNT = 13
}

@Composable
fun UserAvatar(
    avatar: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    shape: Shape = DefaultAvatarShape,
    borderWidth: Dp = 0.8.dp,
    shadowBlur: Dp = 50.dp,
    shadowSpread: Dp = 5.dp,
) {
    val context = LocalContext.current
    val avatarId = remember(avatar) {
        context.getAvatarResource(avatar)
    }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.copy(0.8F)
    val borderColor = remember(avatarId) {
        val drawable = ContextCompat.getDrawable(context, avatarId)!!

        val palette = Palette
            .from(drawable.toBitmap())
            .generate()

        Color(
            palette.darkVibrantSwatch?.titleTextColor
                ?: palette.darkMutedSwatch?.titleTextColor
                ?: onSurfaceColor.toArgb(),
        )
    }

    Box(
        modifier = modifier,
    ) {
        Image(
            painter = painterResource(avatarId),
            contentDescription = stringResource(LocaleR.string.user_avatar),
            contentScale = contentScale,
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = shape,
                )
                .boxShadow(
                    color = MaterialTheme.colorScheme.surface,
                    shape = shape,
                    blurRadius = shadowBlur,
                    spreadRadius = shadowSpread,
                ),
        )
    }
}

@Composable
fun getUserBackgroundPalette(avatar: Int): Palette {
    val context = LocalContext.current

    val avatarId = context.getAvatarResource(avatar)
    val drawable = ContextCompat.getDrawable(context, avatarId)!!

    return remember {
        Palette
            .from(drawable.toBitmap())
            .generate()
    }
}

@Preview
@Composable
private fun UserAvatarPreview() {
    val avatar = remember { Random.nextInt(0, UserAvatarDefaults.AVATARS_IMAGE_COUNT) }
    val swatch = getUserBackgroundPalette(avatar)
        .dominantSwatch
    val defaultColor = MaterialTheme.colorScheme.primary
    val backgroundColor = Color(swatch?.rgb ?: defaultColor.toArgb())

    FlixclusiveTheme {
        val surface = MaterialTheme.colorScheme.surface
        Surface(
            color = backgroundColor.copy(1F),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9F)
                    .fillMaxHeight(0.8F)
                    .drawBehind {
                        drawRect(
                            Brush.verticalGradient(
                                0F to backgroundColor,
                                0.55F to surface.copy(0.8F),
                                1F to surface,
                            ),
                        )
                    },
            ) {
                UserAvatar(
                    avatar = avatar,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .size(DefaultAvatarSize * 2.5F)
                        .align(Alignment.TopCenter)
                        .padding(top = 25.dp),
                )
            }
        }
    }
}
