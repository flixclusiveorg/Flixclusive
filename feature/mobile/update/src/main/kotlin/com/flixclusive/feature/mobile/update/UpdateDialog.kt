package com.flixclusive.feature.mobile.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.ui.common.GradientCircularProgressIndicator
import com.flixclusive.core.ui.common.navigation.navigator.UpdateDialogNavigator
import com.flixclusive.data.configuration.UpdateStatus
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.spec.DestinationStyle
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal object DismissibleDialog : DestinationStyle.Dialog {
    override val properties = DialogProperties(
        dismissOnClickOutside = true,
        dismissOnBackPress = true,
    )
}

@Destination(style = DismissibleDialog::class)
@Composable
internal fun UpdateDialog(
    navigator: UpdateDialogNavigator,
) {
    val viewModel = hiltViewModel<UpdateFeatureViewModel>()
    val updateStatus by viewModel.appUpdateCheckerUseCase.updateStatus.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.appUpdateCheckerUseCase.checkForUpdates()
    }

    LaunchedEffect(updateStatus) {
        if (
            updateStatus is UpdateStatus.Outdated
            && viewModel.appUpdateCheckerUseCase.updateUrl != null
            && viewModel.appUpdateCheckerUseCase.newVersion != null
        ) {
            navigator.openUpdateScreen(
                newVersion = viewModel.appUpdateCheckerUseCase.newVersion!!,
                updateUrl = viewModel.appUpdateCheckerUseCase.updateUrl!!,
                updateInfo = viewModel.appUpdateCheckerUseCase.updateInfo
            )
        }
    }

    Dialog(
        onDismissRequest = navigator::goBack
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Box(
                modifier = Modifier.padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = updateStatus == UpdateStatus.Fetching,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier.matchParentSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(30.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GradientCircularProgressIndicator(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                )
                            )
                        }

                        Text(
                            text = stringResource(id = LocaleR.string.checking_for_updates),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                AnimatedVisibility(
                    visible = updateStatus is UpdateStatus.Error,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier.matchParentSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(30.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = UiCommonR.drawable.round_error_outline_24),
                            contentDescription = stringResource(id = LocaleR.string.error_icon_content_desc),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(80.dp)
                                .padding(bottom = 15.dp)
                        )

                        Text(
                            text = updateStatus.errorMessage?.asString()
                                ?: stringResource(id = LocaleR.string.failed_checking_for_updates),
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                AnimatedVisibility(
                    visible = updateStatus is UpdateStatus.UpToDate,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier.matchParentSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(30.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.round_check_circle_outline_24),
                            contentDescription = "Updated icon",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .size(80.dp)
                                .padding(bottom = 15.dp)
                        )

                        Text(
                            text = stringResource(id = LocaleR.string.up_to_date),
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
