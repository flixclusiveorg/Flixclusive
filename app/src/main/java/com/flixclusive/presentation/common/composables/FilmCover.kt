package com.flixclusive.presentation.common.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import coil.imageLoader
import com.flixclusive.R
import com.flixclusive.presentation.utils.ImageRequestCreator.buildImageUrl
import com.flixclusive.presentation.utils.ModifierUtils.ifElse

enum class FilmCover(val ratio: Float) {
    Backdrop(16F / 9F),
    Poster(2F / 3F);

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    operator fun invoke(
        modifier: Modifier = Modifier,
        imagePath: String?,
        imageSize: String,
        contentDescription: String? = null,
        onClick: (() -> Unit)? = null,
        onLongClick: (() -> Unit)? = null,
    ) {
        val haptic = LocalHapticFeedback.current
        val context = LocalContext.current

        val painter = remember(imagePath) {
            context.buildImageUrl(
                imagePath = imagePath,
                imageSize = imageSize
            )
        }

        val imagePlaceholder = if(ratio == Poster.ratio) painterResource(id = R.drawable.movie_placeholder) else null

        AsyncImage(
            model = painter,
            placeholder = imagePlaceholder,
            imageLoader = LocalContext.current.imageLoader,
            error = imagePlaceholder,
            contentDescription = contentDescription ?: stringResource(id = R.string.film_item_content_description),
            modifier = modifier
                .aspectRatio(ratio)
                .clip(MaterialTheme.shapes.extraSmall)
                .ifElse(
                    condition = onClick != null,
                    ifTrueModifier = Modifier.combinedClickable(
                        onClick = { onClick?.invoke() },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick?.invoke()
                        }
                    )
                )
        )
    }
}