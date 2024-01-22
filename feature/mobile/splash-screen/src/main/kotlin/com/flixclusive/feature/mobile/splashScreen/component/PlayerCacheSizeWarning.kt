package com.flixclusive.feature.mobile.splashScreen.component

import android.text.format.Formatter.formatShortFileSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.theme.warningColor
import com.flixclusive.core.ui.mobile.util.onMediumEmphasis
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.feature.mobile.splashScreen.R
import kotlinx.coroutines.launch
import com.flixclusive.core.util.R as UtilR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerCacheSizeWarning(
    cacheSize: Long,
    onDismiss: (isNotIgnored: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var shouldNotifyAboutCache by remember { mutableStateOf(true) }
    val buttonMinHeight = 60.dp

    AlertDialog(
        onDismissRequest = { onDismiss(shouldNotifyAboutCache) }
    ) {
        Surface(
            shape = RoundedCornerShape(10),
        ) {
            Box(
                modifier = Modifier
                    .heightIn(min = 220.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(10.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.warning),
                        contentDescription = stringResource(
                            id = UtilR.string.warning_content_description
                        ),
                        tint = warningColor,
                        modifier = Modifier
                            .padding(10.dp)
                    )

                    Text(
                        text = stringResource(UtilR.string.too_much_cache),
                        style = MaterialTheme.typography.bodyLarge,
                        color = warningColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                    )

                    Text(
                        text = buildAnnotatedString {
                            append(context.getString(UtilR.string.high_cache_size_warning_message))
                            append(" ")
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(formatShortFileSize(context, cacheSize))
                            }

                            append("\n\n")
                            append(context.getString(UtilR.string.high_cache_size_solution_message))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.onMediumEmphasis(),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    )

                    CustomCheckbox(
                        size = 16.dp,
                        checked = !shouldNotifyAboutCache,
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onPrimary,
                        onCheckedChange = { shouldNotifyAboutCache = !it },
                        modifier = Modifier
                            .padding(top = 10.dp)
                    ) {
                        Text(
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.onMediumEmphasis(),
                            text = stringResource(UtilR.string.ignore_warning_message),
                        )
                    }

                    Row {
                        Button(
                            onClick = {
                                safeCall {
                                    scope.launch { context.cacheDir.deleteRecursively() }
                                    onDismiss(true) // Reset the `shouldNotifyAboutCache` settings
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = warningColor.onMediumEmphasis(),
                                contentColor = Color.Black
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1F)
                                .heightIn(min = buttonMinHeight)
                                .padding(5.dp)
                        ) {
                            Text(
                                text = stringResource(id = UtilR.string.clear),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(end = 2.dp)
                            )
                        }

                        Button(
                            onClick = { onDismiss(shouldNotifyAboutCache) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.onMediumEmphasis()
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1F)
                                .heightIn(min = buttonMinHeight)
                                .padding(5.dp)
                        ) {
                            Text(
                                text = stringResource(id = UtilR.string.close_label),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Light
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PlayerCacheSizeWarningPreview() {
    val cacheSize = 3000 * 1000L * 1000L
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = Color.Gray
        ) {
            PlayerCacheSizeWarning(cacheSize = cacheSize, onDismiss = {})
        }
    }
}