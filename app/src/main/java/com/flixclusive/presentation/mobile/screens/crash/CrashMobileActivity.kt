package com.flixclusive.presentation.mobile.screens.crash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.R
import com.flixclusive.presentation.mobile.main.MainActivity
import com.flixclusive.presentation.mobile.theme.FlixclusiveMobileTheme
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils.colorOnMediumEmphasisMobile
import com.flixclusive.presentation.utils.showToast

class CrashMobileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val errorMessage = intent?.extras?.getString(ERROR_MESSAGE)
        val softwareInfo = intent?.extras?.getString(SOFTWARE_INFO)

        setContent {
            FlixclusiveMobileTheme {
                Surface {
                    CrashScreen(softwareInfo = softwareInfo,
                        errorMessage = errorMessage,
                        onDismiss = {
                            finishAffinity()
                            startActivity(Intent(this, MainActivity::class.java))
                        })
                }
            }
        }

    }
}

@Composable
private fun CrashScreen(
    softwareInfo: String? = null,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val errorMessageToUse = errorMessage ?: stringResource(R.string.default_error_message)
    val copyMessage = stringResource(R.string.copy_stack_trace_message)

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(20.dp)
    ) {
        Text(
            text = stringResource(id = R.string.error_crash_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 80.sp, fontWeight = FontWeight.Bold
            )
        )

        Text(
            text = stringResource(id = R.string.something_went_wrong),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 30.sp, fontWeight = FontWeight.Bold
            )
        )

        Divider(
            modifier = Modifier.padding(top = 15.dp, bottom = 5.dp),
            thickness = 0.5.dp,
            color = colorOnMediumEmphasisMobile(emphasis = 0.3F)
        )

        softwareInfo?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp, fontWeight = FontWeight.Normal
                ),
            )

            Divider(
                modifier = Modifier.padding(vertical = 5.dp),
                thickness = 0.5.dp,
                color = colorOnMediumEmphasisMobile(emphasis = 0.3F)
            )
        }

        Box(
            modifier = Modifier.weight(1F)
        ) {
            TextField(
                value = errorMessageToUse,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxSize(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Normal
                ),
                shape = MaterialTheme.shapes.extraSmall,
                readOnly = true,
                colors = TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                )
            )

            Surface(
                tonalElevation = 30.dp,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(45.dp)
                        .clip(MaterialTheme.shapes.small)
                        .clickable {
                            clipboardManager.setText(
                                AnnotatedString(errorMessageToUse)
                            )
                            context.showToast(copyMessage)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.round_content_copy_24),
                        contentDescription = "Copy button for error stack trace",
                    )
                }
            }
        }

        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = colorOnMediumEmphasisMobile(Color.White)
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 70.dp)
                .padding(vertical = 5.dp)
        ) {
            Text(
                text = stringResource(R.string.restart),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp
                ),
                fontWeight = FontWeight.Normal

            )
        }
    }
}

@Preview(
    device = "id:Realme 5", showSystemUi = true,
    showBackground = true, backgroundColor = 0xFFFFFFFF, fontScale = 1.0f,
)
@Composable
fun CrashScreenPreview() {
    FlixclusiveMobileTheme {
        Surface(
            color = MaterialTheme.colorScheme.surface
        ) {
            CrashScreen(
                softwareInfo = "• SDK: 29\n• Build: Release",
                errorMessage = IllegalStateException("Error cannot be an instance of an exception. Please read the docs mf.").stackTraceToString()
            ) {

            }
        }
    }
}