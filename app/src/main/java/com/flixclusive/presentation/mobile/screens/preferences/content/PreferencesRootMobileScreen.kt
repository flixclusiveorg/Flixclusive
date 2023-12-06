package com.flixclusive.presentation.mobile.screens.preferences.content

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.flixclusive.R
import com.flixclusive.common.Constants.GITHUB_REPOSITORY
import com.flixclusive.presentation.common.FadeInAndOutScreenTransition
import com.flixclusive.presentation.mobile.main.LABEL_START_PADDING
import com.flixclusive.presentation.mobile.screens.preferences.PreferencesNavGraph
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils.colorOnMediumEmphasisMobile
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@PreferencesNavGraph(start = true)
@Destination(
    style = FadeInAndOutScreenTransition::class
)
@Composable
fun PreferencesRootMobileScreen(
    navigator: DestinationsNavigator
) {
    val items = remember { PreferencesItems.values() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = LABEL_START_PADDING),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(id = R.string.preferences),
                    style = MaterialTheme.typography.headlineMedium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }
        }

        item {
            PreferencesRootHeader()
        }

        itemsIndexed(items) { i, item ->
            PreferencesItem(
                iconId = item.iconId,
                labelId = item.labelId,
                onClick = {
                    item.direction?.let {
                        navigator.navigate(it, onlyIfResumed = true)
                    }
                }
            )

            if (i < items.lastIndex)
                Divider(
                    color = colorOnMediumEmphasisMobile(emphasis = 0.3F),
                    thickness = 1.dp,
                    modifier = Modifier
                        .padding(horizontal = 25.dp)
                )
        }
    }
}

@Composable
private fun PreferencesItem(
    @DrawableRes iconId: Int,
    @StringRes labelId: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(35.dp)
                .padding(start = LABEL_START_PADDING),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconId),
                contentDescription = stringResource(id = labelId)
            )
        }

        Text(
            text = stringResource(id = labelId),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            ),
            modifier = Modifier
                .padding(start = 12.dp)
        )
    }
}

@Composable
private fun PreferencesRootHeader() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Row(
        modifier = Modifier
            .padding(
                horizontal = LABEL_START_PADDING,
                vertical = 20.dp
            )
            .background(
                colorOnMediumEmphasisMobile(emphasis = 0.2F),
                RoundedCornerShape(15)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.share_the_app),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            ),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
                .weight(1F)
                .padding(start = LABEL_START_PADDING)
        )

        HeaderButton(
            iconId = R.drawable.round_content_copy_24,
            contentDescription = "Copy Button",
            onClick =  {
                clipboardManager.setText(AnnotatedString(GITHUB_REPOSITORY))
            }
        )

        HeaderButton(
            iconId = R.drawable.round_share_24,
            contentDescription = "Share Button",
            onClick =  {
                val type = "text/plain"
                val subject = "Flixclusive App Link"
                val extraText = "Watch all latest exclusive films at:\n\n$GITHUB_REPOSITORY"
                val shareWith = "ShareWith"

                val intent = Intent(Intent.ACTION_SEND)
                    .apply {
                        this.type = type
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, extraText)
                    }

                ContextCompat.startActivity(
                    context,
                    Intent.createChooser(intent, shareWith),
                    null
                )
            }
        )
    }
}

@Composable
private fun HeaderButton(
    modifier: Modifier = Modifier,
    size: Dp = 45.dp,
    @DrawableRes iconId: Int,
    contentDescription: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .background(color = Color.Transparent)
            .clickable(
                onClick = onClick,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(
                    bounded = false,
                    radius = size / 2
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = contentDescription
        )
    }
}
