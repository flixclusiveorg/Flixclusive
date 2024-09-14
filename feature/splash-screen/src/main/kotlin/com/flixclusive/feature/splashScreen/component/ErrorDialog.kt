package com.flixclusive.feature.splashScreen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.locale.R as LocaleR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ErrorDialog(
    title: String,
    description: String,
    dismissButtonLabel: String = stringResource(LocaleR.string.exit),
    onDismiss: () -> Unit,
) {
    val buttonMinHeight = 60.dp

    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(10)
        ) {
            Box(
                modifier = Modifier
                    .height(220.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(10.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                    )

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.onMediumEmphasis(),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1F)
                    )

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = Color.White.onMediumEmphasis()
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = buttonMinHeight)
                            .padding(5.dp)
                    ) {
                        Text(
                            text = dismissButtonLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ErrorDialogPreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = Color.Gray
        ) {
            ErrorDialog(title = "Cache warning!", description = "Sample error") {

            }
        }
    }
}