package com.flixclusive.presentation.mobile.screens.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.flixclusive.R
import com.flixclusive.presentation.mobile.theme.FlixclusiveMobileTheme
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils
import com.flixclusive.service.app_updater.AppUpdaterService.Companion.startAppUpdater
import com.flixclusive.service.utils.IntentUtils

const val UPDATE_PROGRESS_RECEIVER_ACTION = "update_progress_receiver_action"
const val UPDATE_PROGRESS = "update_progress"
const val UPDATE_LOCATION = "update_location"
const val UPDATE_NEW_VERSION = "update_new_version"
const val UPDATE_URL = "update_url"
const val UPDATE_INFORMATION = "update_info"

class UpdateMobileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val newVersion = intent?.getStringExtra(UPDATE_NEW_VERSION)
        val updateUrl = intent?.getStringExtra(UPDATE_URL)
        val updateInfo = intent?.getStringExtra(UPDATE_INFORMATION)

        setContent {
            FlixclusiveMobileTheme {
                Surface {
                    UpdateScreen(
                        newVersion = newVersion!!,
                        updateInfo = updateInfo,
                        onUpdate = {
                            startAppUpdater(updateUrl!!)
                        },
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateScreen(
    newVersion: String,
    updateInfo: String? = null,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    val buttonPaddingValues = PaddingValues(horizontal = 5.dp, vertical = 10.dp)
    val brushGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
        )
    )
    val progressColor = MaterialTheme.colorScheme.primary

    var progress by remember { mutableStateOf<Int?>(null) }
    val progressState by animateFloatAsState(
        targetValue = progress?.div(100F) ?: 0F,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = ""
    )
    var updateUri by remember { mutableStateOf<Uri?>(null) }
    val readyToInstall = remember(progress, updateUri) {
        progress != null && updateUri != null && progress!! >= 100
    }

    val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val receivedProgress = intent.getIntExtra(UPDATE_PROGRESS, -1)
            val receivedUpdateUri = intent.getStringExtra(UPDATE_LOCATION)

            receivedUpdateUri?.let { updateUri = Uri.parse(receivedUpdateUri) }
            progress = if (receivedProgress == -1) progress else receivedProgress
        }
    }

    LocalBroadcastManager.getInstance(context).registerReceiver(
        broadcastReceiver, IntentFilter(UPDATE_PROGRESS_RECEIVER_ACTION)
    )

    Box {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .padding(
                    bottom = 20.dp,
                    end = 20.dp,
                    start = 20.dp
                )
                .verticalScroll(rememberScrollState())
        ) {
            Image(
                painter = painterResource(id = R.drawable.flixclusive_tag),
                contentDescription = "Flixclusive Tag",
                contentScale = ContentScale.FillHeight,
                alignment = Alignment.CenterStart,
                modifier = Modifier
                    .height(100.dp)
                    .graphicsLayer(alpha = 0.99f)
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            drawRect(brushGradient, blendMode = BlendMode.SrcAtop)
                        }
                    }
            )

            Text(
                text = "$newVersion is out now!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 25.sp, fontWeight = FontWeight.Bold
                )
            )

            Divider(
                modifier = Modifier.padding(top = 15.dp, bottom = 5.dp),
                thickness = 0.5.dp,
                color = ComposeMobileUtils.colorOnMediumEmphasisMobile(emphasis = 0.3F)
            )

            Text(
                text = stringResource(id = R.string.update_info),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp, fontWeight = FontWeight.Medium
                ),
            )

            Text(
                text = updateInfo ?: stringResource(id = R.string.default_update_info_message),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp, fontWeight = FontWeight.Normal
                ),
            )
        }



        AnimatedVisibility(
            visible = progress == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Button(
                    onClick = {
                        if (readyToInstall) {
                            val installIntent = IntentUtils.installApkActivity(updateUri!!)
                            context.startActivity(installIntent)
                            onDismiss()
                        } else onUpdate()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(0.5F)
                        .heightIn(min = 70.dp)
                        .padding(buttonPaddingValues)
                ) {
                    Text(
                        text = stringResource(R.string.update_label),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 16.sp
                        ),
                        fontWeight = FontWeight.Normal
                    )
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = ComposeMobileUtils.colorOnMediumEmphasisMobile(Color.White)
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(0.5F)
                        .heightIn(min = 70.dp)
                        .padding(buttonPaddingValues)
                ) {
                    Text(
                        text = stringResource(R.string.not_now_label),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 16.sp
                        ),
                        fontWeight = FontWeight.Normal

                    )
                }
            }
        }

        AnimatedVisibility(
            visible = progress != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                var labelToUse = stringResource(R.string.update_label)

                if (readyToInstall) {
                    labelToUse = "Install"
                } else if (progress != null && progress!! > -1) {
                    labelToUse = "Updating [$progress%]"
                }

                Box(
                    modifier = Modifier
                        .padding(buttonPaddingValues),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.medium
                            )
                            .clip(MaterialTheme.shapes.medium)
                            .fillMaxWidth()
                            .height(55.dp)
                            .clickable(readyToInstall) {
                                if (readyToInstall) {
                                    val installIntent = IntentUtils.installApkActivity(updateUri!!)
                                    context.startActivity(installIntent)
                                }
                            }
                            .drawWithContent {
                                with(drawContext.canvas.nativeCanvas) {
                                    val checkPoint = saveLayer(null, null)

                                    drawContent()
                                    drawRect(
                                        color = progressColor,
                                        size = Size(size.width * progressState, size.height),
                                        blendMode = BlendMode.SrcOut
                                    )
                                    restoreToCount(checkPoint)
                                }
                            }
                    ) {
                        Text(
                            text = labelToUse,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}