package com.flixclusive.presentation.mobile.screens.preferences.update

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.R
import com.flixclusive.presentation.common.viewmodels.config.AppConfigurationViewModel
import com.flixclusive.presentation.mobile.common.composables.GradientCircularProgressIndicator
import com.flixclusive.presentation.mobile.screens.preferences.PreferencesNavGraph
import com.flixclusive.presentation.mobile.screens.update.UPDATE_INFORMATION
import com.flixclusive.presentation.mobile.screens.update.UPDATE_NEW_VERSION
import com.flixclusive.presentation.mobile.screens.update.UPDATE_URL
import com.flixclusive.presentation.mobile.screens.update.UpdateMobileActivity
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.DestinationStyle

object DismissibleDialog : DestinationStyle.Dialog {
    override val properties = DialogProperties(
        dismissOnClickOutside = true,
        dismissOnBackPress = true,
    )
}
@PreferencesNavGraph
@Destination(style = DismissibleDialog::class)
@Composable
fun UpdateDialogScreen(
    navigator: DestinationsNavigator,
) {
    val viewModel = hiltViewModel<AppConfigurationViewModel>()
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.checkForUpdates()
    }

    LaunchedEffect(uiState.isDoneInitializing, uiState.isNeedingAnUpdate) {
        if (!uiState.isDoneInitializing && uiState.isNeedingAnUpdate) {
            context.startActivity(
                Intent(context, UpdateMobileActivity::class.java).apply {
                    putExtra(UPDATE_URL, uiState.updateUrl)
                    putExtra(UPDATE_NEW_VERSION, uiState.newVersion)
                    putExtra(UPDATE_INFORMATION, uiState.updateInfo)
                }
            )
            navigator.navigateUp()
        }
    }

    Dialog(
        onDismissRequest = navigator::navigateUp
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
                    visible = !uiState.isDoneInitializing && !uiState.isNeedingAnUpdate,
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
                            GradientCircularProgressIndicator()
                        }

                        Text(
                            text = stringResource(id = R.string.checking_for_updates),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier.matchParentSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(30.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.round_error_outline_24),
                            contentDescription = "Error icon",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(80.dp)
                                .padding(bottom = 15.dp)
                        )

                        Text(
                            text = uiState.errorMessage?.asString()
                                ?: stringResource(id = R.string.failed_checking_for_updates),
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                AnimatedVisibility(
                    visible = uiState.isDoneInitializing && !uiState.isNeedingAnUpdate,
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
                            text = stringResource(id = R.string.up_to_date),
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
