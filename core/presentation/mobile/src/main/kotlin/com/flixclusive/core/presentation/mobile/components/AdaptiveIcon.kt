package com.flixclusive.core.presentation.mobile.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toolingGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import kotlin.math.max

/**
 * A modified platform adaptive version of [Icon].
 *
 * @param imageVector [ImageVector] to draw inside this icon
 * @param contentDescription text used by accessibility services to describe what this icon
 *   represents. This should always be provided unless this icon is used for decorative purposes,
 *   and does not represent a meaningful action that a user can take. This text should be localized,
 *   such as by using [androidx.compose.ui.res.stringResource] or similar
 * @param modifier the [Modifier] to be applied to this icon
 * @param tint tint to be applied to [imageVector]. If [Color.Unspecified] is provided, then no tint
 *   is applied.
 * @param compact the DP size to use for compact screen sizes
 * @param medium the DP size to use for medium screen sizes
 * @param expanded the DP size to use for expanded screen sizes
 */
@Composable
fun AdaptiveIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    compact: Dp? = null,
    medium: Dp? = null,
    expanded: Dp? = null,
) {
    AdaptiveIcon(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
        compact = compact,
        medium = medium,
        expanded = expanded,
    )
}

/**
 * A modified platform adaptive version of [Icon].
 *
 * @param imageVector [ImageVector] to draw inside this icon
 * @param contentDescription text used by accessibility services to describe what this icon
 *   represents. This should always be provided unless this icon is used for decorative purposes,
 *   and does not represent a meaningful action that a user can take. This text should be localized,
 *   such as by using [androidx.compose.ui.res.stringResource] or similar
 * @param modifier the [Modifier] to be applied to this icon
 * @param tint tint to be applied to [imageVector]. If [Color.Unspecified] is provided, then no tint
 *   is applied.
 * @param dp the base DP size to use for compact screens
 * @param increaseBy the amount to increase the [dp] size per each window size class
 */
@Composable
fun AdaptiveIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    dp: Dp? = null,
    increaseBy: Dp = DefaultIconSizeIncreaseBy,
) {
    AdaptiveIcon(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
        dp = dp,
        increaseBy = increaseBy,
    )
}

/**
 * A modified platform adaptive version of [Icon].
 *
 * @param bitmap [ImageBitmap] to draw inside this icon
 * @param contentDescription text used by accessibility services to describe what this icon
 *   represents. This should always be provided unless this icon is used for decorative purposes,
 *   and does not represent a meaningful action that a user can take. This text should be localized,
 *   such as by using [androidx.compose.ui.res.stringResource] or similar
 * @param modifier the [Modifier] to be applied to this icon
 * @param tint tint to be applied to [bitmap]. If [Color.Unspecified] is provided, then no tint is
 *   applied.
 * @param compact the DP size to use for compact screen sizes
 * @param medium the DP size to use for medium screen sizes
 * @param expanded the DP size to use for expanded screen sizes
 */
@Composable
fun AdaptiveIcon(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    compact: Dp? = null,
    medium: Dp? = null,
    expanded: Dp? = null,
) {
    val painter = remember(bitmap) { BitmapPainter(bitmap) }
    AdaptiveIcon(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
        compact = compact,
        medium = medium,
        expanded = expanded,
    )
}

/**
 * A modified platform adaptive version of [Icon].
 *
 * @param bitmap [ImageBitmap] to draw inside this icon
 * @param contentDescription text used by accessibility services to describe what this icon
 *   represents. This should always be provided unless this icon is used for decorative purposes,
 *   and does not represent a meaningful action that a user can take. This text should be localized,
 *   such as by using [androidx.compose.ui.res.stringResource] or similar
 * @param modifier the [Modifier] to be applied to this icon
 * @param tint tint to be applied to [bitmap]. If [Color.Unspecified] is provided, then no tint is
 *   applied.
 * @param dp the base DP size to use for compact screens
 * @param increaseBy the amount to increase the [dp] size per each window size class
 */
@Composable
fun AdaptiveIcon(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    dp: Dp? = null,
    increaseBy: Dp = DefaultIconSizeIncreaseBy,
) {
    val painter = remember(bitmap) { BitmapPainter(bitmap) }
    AdaptiveIcon(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
        dp = dp,
        increaseBy = increaseBy,
    )
}

/**
 * A modified platform adaptive version of [Icon].
 *
 * @param painter [Painter] to draw inside this icon
 * @param contentDescription text used by accessibility services to describe what this icon
 *   represents. This should always be provided unless this icon is used for decorative purposes,
 *   and does not represent a meaningful action that a user can take. This text should be localized,
 *   such as by using [androidx.compose.ui.res.stringResource] or similar
 * @param modifier the [Modifier] to be applied to this icon
 * @param tint tint to be applied to [painter]. If [Color.Unspecified] is provided, then no tint is applied.
 * @param compact the DP size to use for compact screen sizes
 * @param medium the DP size to use for medium screen sizes
 * @param expanded the DP size to use for expanded screen sizes
 */
@Composable
fun AdaptiveIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    compact: Dp? = null,
    medium: Dp? = null,
    expanded: Dp? = null,
) {
    val colorFilter =
        remember(tint) { if (tint == Color.Unspecified) null else ColorFilter.tint(tint) }
    val semantics =
        if (contentDescription != null) {
            Modifier.semantics {
                this.contentDescription = contentDescription
                this.role = Role.Image
            }
        } else {
            Modifier
        }

    Box(
        modifier
            .toolingGraphicsLayer()
            .defaultSizeFor(
                painter = painter,
                compact = compact,
                medium = medium,
                expanded = expanded,
            ).paint(painter, colorFilter = colorFilter, contentScale = ContentScale.Fit)
            .then(semantics),
    )
}

/**
 * A modified platform adaptive version of [Icon].
 *
 * @param painter [Painter] to draw inside this icon
 * @param contentDescription text used by accessibility services to describe what this icon
 *   represents. This should always be provided unless this icon is used for decorative purposes,
 *   and does not represent a meaningful action that a user can take. This text should be localized,
 *   such as by using [androidx.compose.ui.res.stringResource] or similar
 * @param modifier the [Modifier] to be applied to this icon
 * @param tint tint to be applied to [painter]. If [Color.Unspecified] is provided, then no tint is applied.
 * @param dp the base DP size to use for compact screens
 * @param increaseBy the amount to increase the [dp] size per each window size class
 */
@Composable
fun AdaptiveIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    dp: Dp? = null,
    increaseBy: Dp = DefaultIconSizeIncreaseBy,
) {
    val compact = dp ?: painter.getSizeInDp() ?: DefaultIconSize

    AdaptiveIcon(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
        compact = compact,
        medium = compact + increaseBy,
        expanded = compact + (increaseBy * 2),
    )
}

@Composable
private fun Modifier.defaultSizeFor(
    painter: Painter,
    compact: Dp?,
    medium: Dp?,
    expanded: Dp?,
): Modifier {
    return if (painter.hasNoValidSize() || compact != null) {
        val baseDp = compact ?: DefaultIconSize
        val (mediumDp, expandedDp) = getMediumAndExpandedDp(
            compact = baseDp,
            medium = medium,
            expanded = expanded,
        )

        size(
            getAdaptiveDp(
                compact = baseDp,
                medium = mediumDp,
                expanded = expandedDp,
            ),
        )
    } else {
        val sizeInDp = painter.getSizeInDp()
            ?: throw NullPointerException("This Icon's painter object has no valid size.")
        val (mediumDp, expandedDp) = getMediumAndExpandedDp(
            compact = sizeInDp,
            medium = medium,
            expanded = expanded,
        )

        size(
            getAdaptiveDp(
                compact = sizeInDp,
                medium = mediumDp,
                expanded = expandedDp,
            ),
        )
    }
}

@Composable
private fun Painter.getSizeInDp(): Dp? {
    if (hasNoValidSize()) {
        return null
    }

    val painterSize = max(intrinsicSize.width, intrinsicSize.height)
    return with(LocalDensity.current) { painterSize.toDp() }
}

private fun Size.isInfinite() = width.isInfinite() && height.isInfinite()

private fun Painter.hasNoValidSize() = intrinsicSize == Size.Unspecified || intrinsicSize.isInfinite()

private fun getMediumAndExpandedDp(
    compact: Dp,
    medium: Dp?,
    expanded: Dp?,
): Pair<Dp, Dp> {
    val mediumDp = medium ?: (compact + DefaultIconSizeIncreaseBy)
    val expandedDp = expanded ?: (mediumDp + DefaultIconSizeIncreaseBy)

    return mediumDp to expandedDp
}

// Default icon size, for icons with no intrinsic size information
private val DefaultIconSize = 24.dp
private val DefaultIconSizeIncreaseBy = 6.dp
