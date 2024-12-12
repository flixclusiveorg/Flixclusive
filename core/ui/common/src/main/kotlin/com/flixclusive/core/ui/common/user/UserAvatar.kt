package com.flixclusive.core.ui.common.user

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.boxShadow
import com.flixclusive.model.database.User
import com.flixclusive.core.locale.R as LocaleR

val DefaultAvatarSize = 100.dp
const val AVATAR_PREFIX = "avatar"
const val AVATARS_IMAGE_COUNT = 10

@Composable
fun UserAvatar(
    modifier: Modifier = Modifier,
    user: User,
    boxShadowBlur: Dp = 16.dp
) {
    val context = LocalContext.current
    val avatarId = remember(user.image) {
        context.getAvatarResource(user.image)
    }

    val shape = MaterialTheme.shapes.small
    val primaryColor = MaterialTheme.colorScheme.primary
    val borderColor = remember(avatarId) {
        val drawable = ContextCompat.getDrawable(context, avatarId)!!

        val palette = Palette
            .from(drawable.toBitmap())
            .generate()

        val swatch = palette.let {
            it.vibrantSwatch
                ?: it.lightVibrantSwatch
                ?: it.lightMutedSwatch
        }

        swatch?.rgb?.let { Color(it) }
            ?: primaryColor
    }

    Box(
        modifier = modifier
    ) {
        Image(
            painter = painterResource(avatarId),
            contentDescription = stringResource(LocaleR.string.user_avatar),
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 0.5.dp,
                    color = borderColor.copy(0.6F),
                    shape = shape
                )
                .boxShadow(
                    color = MaterialTheme.colorScheme.surface.copy(0.5F),
                    shape = shape,
                    blurRadius = boxShadowBlur
                )
        )
    }
}

@SuppressLint("DiscouragedApi")
fun Context.getAvatarResource(imageIndex: Int): Int {
    val resourceName = "$AVATAR_PREFIX$imageIndex"
    val id = resources.getIdentifier(resourceName, "drawable", packageName)

    require(id != 0) {
        "Avatar image could not be found: avatar$imageIndex"
    }

    return id
}

@Preview
@Composable
private fun UserAvatarPreview() {
    val context = LocalContext.current
    val avatarId = remember {
        context.getAvatarResource(0)
    }

    val backgroundColor = remember {
        val drawable = ContextCompat.getDrawable(context, avatarId)!!

        Palette
            .from(drawable.toBitmap())
            .generate()
            .lightVibrantSwatch!!
            .rgb
    }

    FlixclusiveTheme {
        val surface = MaterialTheme.colorScheme.surface
        val largeRadialGradient = object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val biggerDimension = maxOf(size.height, size.width)
                return RadialGradientShader(
                    colors = listOf(
                        Color(backgroundColor).copy(alpha = 0.05F),
                        surface
                    ),
                    center = size.center,
                    radius = biggerDimension / 2f,
                    colorStops = listOf(0f, 0.95f)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DefaultAvatarSize * 3)
                .background(largeRadialGradient),
            contentAlignment = Alignment.Center
        ) {
            Box {
                UserAvatar(
                    user = User(image = 0),
                    modifier = Modifier
                        .size(DefaultAvatarSize)
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@Preview
@Composable
private fun UserAvatarPreview2() {
    val context = LocalContext.current
    val avatarId = remember {
        context.getAvatarResource(1)
    }
    val backgroundColor = remember {
        val drawable = ContextCompat.getDrawable(context, avatarId)!!

        Palette
            .from(drawable.toBitmap())
            .generate()
            .lightVibrantSwatch!!
            .rgb
    }

    FlixclusiveTheme {
        val surface = MaterialTheme.colorScheme.surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DefaultAvatarSize * 3)
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            0.2F to Color(backgroundColor).copy(0.05F),
                            0.8F to surface
                        )
                    )
                }
        ) {
            UserAvatar(
                user = User(image = 1),
                modifier = Modifier
                    .size(DefaultAvatarSize)
                    .align(Alignment.Center)
            )
        }
    }
}
