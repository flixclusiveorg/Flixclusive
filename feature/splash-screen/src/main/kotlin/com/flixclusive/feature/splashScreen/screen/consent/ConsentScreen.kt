package com.flixclusive.feature.splashScreen.screen.consent

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.mobile.component.CustomCheckbox
import com.flixclusive.feature.splashScreen.ENTER_DELAY
import com.flixclusive.feature.splashScreen.EXIT_DELAY
import com.flixclusive.feature.splashScreen.PaddingHorizontal
import com.flixclusive.feature.splashScreen.component.LoadingTag
import com.flixclusive.feature.splashScreen.component.Tag
import kotlinx.coroutines.delay
import com.flixclusive.core.locale.R as LocaleR

private fun getEnterAnimation(delay: Int): EnterTransition {
    return fadeIn(
        animationSpec =
            tween(
                durationMillis = ENTER_DELAY,
                delayMillis = delay,
            ),
    ) +
        slideInHorizontally(
            tween(delayMillis = delay),
        )
}

private fun getExitAnimation(delay: Int): ExitTransition {
    return fadeOut(
        animationSpec =
            tween(
                delayMillis = delay,
            ),
    ) +
        slideOutHorizontally(
            animationSpec =
                tween(
                    durationMillis = EXIT_DELAY,
                    delayMillis = delay,
                ),
        )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ConsentScreen(
    modifier: Modifier = Modifier,
    animatedScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    onAgree: (isOptingIn: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val consents = remember { context.getConsents() }

    val delayMs = ENTER_DELAY
    val isOptingIn = remember { mutableStateOf(true) }

    Box(modifier = modifier) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize(),
            contentPadding = PaddingValues(vertical = PaddingHorizontal),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            item {
                Tag(
                    animatedScope = animatedScope,
                    sharedTransitionScope = sharedTransitionScope,
                )
            }

            itemsIndexed(consents) { i, it ->
                val enterDelay = (i * delayMs) / 2
                val exitDelay = ((consents.size - i) * delayMs) / 5

                with(animatedScope) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(15.dp, Alignment.CenterVertically),
                        modifier =
                            Modifier.animateEnterExit(
                                enter = getEnterAnimation(enterDelay),
                                exit = getExitAnimation(exitDelay),
                            ),
                    ) {
                        HorizontalDivider(
                            modifier =
                                Modifier
                                    .padding(vertical = 8.dp),
                            thickness = 0.5.dp,
                        )

                        HeaderBodyComponent(
                            consent = it,
                            isOptingIn = isOptingIn,
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(60.dp))
            }
        }

        with(animatedScope) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                0F to Color.Transparent,
                                1F to MaterialTheme.colorScheme.surface,
                            ),
                        ),
            ) {
                ElevatedButton(
                    onClick = { onAgree(isOptingIn.value) },
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 65.dp)
                            .padding(getAdaptiveDp(5.dp))
                            .animateEnterExit(
                                enter = getEnterAnimation(1100),
                                exit = getExitAnimation(0),
                            ),
                ) {
                    Text(
                        text = stringResource(id = LocaleR.string.understood),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

internal fun Context.getConsents(): List<Consent> {
    return listOf(
        Consent(
            title = getString(LocaleR.string.privacy_notice),
            description = getString(LocaleR.string.privacy_notice_crash_log_sender),
            optInMessage = getString(LocaleR.string.privacy_notice_opt_in),
        ),
        Consent(
            title = getString(LocaleR.string.providers_disclaimer),
            description = getString(LocaleR.string.provider_disclaimer_message),
        ),
        Consent(
            title = getString(LocaleR.string.gen_ai_notice),
            description = getString(LocaleR.string.gen_ai_disclaimer_message),
        ),
    )
}

@Composable
private fun HeaderBodyComponent(
    consent: Consent,
    isOptingIn: MutableState<Boolean>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier =
                Modifier
                    .padding(bottom = 10.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                    ),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier =
                    Modifier
                        .padding(8.dp),
            ) {
                Text(
                    text = consent.title,
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                        ),
                )

                Text(
                    text = consent.description,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                )
            }
        }

        consent.optInMessage?.let {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier =
                    Modifier
                        .padding(top = 5.dp)
                        .align(Alignment.CenterHorizontally),
            ) {
                CustomCheckbox(
                    checked = isOptingIn.value,
                    onCheckedChange = {
                        isOptingIn.value = !isOptingIn.value
                    },
                )

                Text(
                    text = it,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
private fun ConsentPreview() {
    var isLoading by remember { mutableStateOf<Boolean?>(false) }

    LaunchedEffect(isLoading) {
        if (isLoading == true) {
            delay(3000L)
            isLoading = false
        } else if (isLoading == null) {
            delay(3000L)
            isLoading = true
        }
    }

    FlixclusiveTheme {
        Surface(
            modifier =
                Modifier
                    .fillMaxSize(),
        ) {
            SharedTransitionLayout {
                AnimatedContent(
                    isLoading,
                    transitionSpec = {
                        EnterTransition.None
                            .togetherWith(ExitTransition.None)
                    },
                    label = "test_transition",
                ) {
                    if (it == true || it == null) {
                        LoadingTag(
                            isLoading = it ?: false,
                            animatedScope = this@AnimatedContent,
                            sharedTransitionScope = this@SharedTransitionLayout,
                        )
                    } else if (it == false) {
                        ConsentScreen(
                            animatedScope = this@AnimatedContent,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            onAgree = { isLoading = null },
                        )
                    }
                }
            }
        }
    }
}
