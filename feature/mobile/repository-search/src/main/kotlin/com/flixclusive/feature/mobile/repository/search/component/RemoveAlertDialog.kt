package com.flixclusive.feature.mobile.repository.search.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
internal fun RemoveAlertDialog(
    confirm: () -> Unit,
    cancel: () -> Unit,
) {
    val buttonMinHeight = 60.dp

    AlertDialog(
        onDismissRequest = cancel
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
                        text = stringResource(id = LocaleR.string.remove_repositories),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                    )

                    Text(
                        text = stringResource(id = LocaleR.string.remove_repositories_notice_msg),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.onMediumEmphasis(),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1F)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Button(
                            onClick = confirm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.onMediumEmphasis(0.4F),
                                contentColor = Color.White.onMediumEmphasis(0.8F)
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1F)
                                .heightIn(min = buttonMinHeight)
                                .padding(5.dp)
                        ) {
                            Text(
                                text = stringResource(LocaleR.string.remove),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = cancel,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = Color.White.onMediumEmphasis()
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1F)
                                .heightIn(min = buttonMinHeight)
                                .padding(5.dp)
                        ) {
                            Text(
                                text = stringResource(LocaleR.string.cancel),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Light,
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
private fun RemoveAlertDialogPreview() {
    FlixclusiveTheme {
        Surface {
            RemoveAlertDialog(confirm = { /*TODO*/ }) {

            }
        }
    }
}