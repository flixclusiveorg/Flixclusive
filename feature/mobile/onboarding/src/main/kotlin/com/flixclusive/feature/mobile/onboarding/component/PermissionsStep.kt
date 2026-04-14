package com.flixclusive.feature.mobile.onboarding.component

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.material3.CommonBottomSheet
import com.flixclusive.core.presentation.mobile.theme.MobileColors.surfaceColorAtElevation
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.feature.mobile.onboarding.R

internal data class GrantedPermissionItem(
    val label: String,
    val description: String,
)

@Composable
internal fun PermissionsStep(
    notificationsGranted: Boolean,
    unknownSourcesAllowed: Boolean,
    grantedPermissions: List<GrantedPermissionItem>,
    requestNotificationsPermission: () -> Unit,
    openUnknownSourcesSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showGrantedPermissionsSheet by rememberSaveable { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.onboarding_permissions_title),
            style = MaterialTheme.typography.headlineMedium.asAdaptiveTextStyle(),
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = stringResource(R.string.onboarding_permissions_desc),
            style = MaterialTheme.typography.bodyMedium.asAdaptiveTextStyle(),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        )

        PermissionToggleCard(
            title = stringResource(R.string.onboarding_permission_unknown_sources_title),
            badgeLabel = stringResource(R.string.permission_unknown_sources_desc),
            checked = unknownSourcesAllowed,
            enabled = true,
            isRequired = true,
            onToggle = openUnknownSourcesSettings,
        )

        val notificationsChecked = if (Build.VERSION.SDK_INT >= 33) notificationsGranted else true

        if (Build.VERSION.SDK_INT >= 33) {
            PermissionToggleCard(
                title = stringResource(R.string.onboarding_permission_notifications_title),
                badgeLabel = stringResource(R.string.permission_notifications_desc),
                checked = notificationsChecked,
                enabled = !notificationsGranted,
                isRequired = false,
                onToggle = requestNotificationsPermission,
            )
        }

        if (grantedPermissions.isNotEmpty()) {
            TextButton(
                onClick = { showGrantedPermissionsSheet = true },
                modifier = Modifier.align(Alignment.Start),
            ) {
                Text(text = stringResource(R.string.onboarding_permissions_view_granted_button))
            }
        }
    }

    if (showGrantedPermissionsSheet) {
        GrantedPermissionsBottomSheet(
            permissions = grantedPermissions,
            onDismissRequest = { showGrantedPermissionsSheet = false },
        )
    }
}

@Composable
private fun PermissionToggleCard(
    title: String,
    badgeLabel: String,
    checked: Boolean,
    enabled: Boolean,
    isRequired: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onToggle,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(level = 2),
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = buildAnnotatedString {
                        append(title)
                        if (isRequired) {
                            append(" ")
                            withStyle(
                                MaterialTheme.typography.bodySmall.asAdaptiveTextStyle().toSpanStyle().copy(
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                )
                            ) {
                                append("*")
                            }
                        }
                    },
                    style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.bodySmall.asAdaptiveTextStyle(),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                )
            }

            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = { onToggle() },
            )
        }
    }
}

@Composable
private fun GrantedPermissionsBottomSheet(
    permissions: List<GrantedPermissionItem>,
    onDismissRequest: () -> Unit,
) {
    CommonBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_permissions_granted_sheet_title),
                        style = MaterialTheme.typography.titleLarge.asAdaptiveTextStyle(),
                        fontWeight = FontWeight.Bold,
                    )

                    Text(
                        text = stringResource(R.string.onboarding_permissions_granted_sheet_desc),
                        style = MaterialTheme.typography.bodySmall.asAdaptiveTextStyle(),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                    )
                }
            }

            if (permissions.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(level = 1),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_permissions_granted_sheet_empty),
                            style = MaterialTheme.typography.bodyMedium.asAdaptiveTextStyle(),
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }

            itemsIndexed(permissions) { i, permission ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = permission.label,
                        style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
                        fontWeight = FontWeight.SemiBold,
                    )

                    Text(
                        text = permission.description,
                        style = MaterialTheme.typography.bodySmall.asAdaptiveTextStyle(),
                        color = LocalContentColor.current.copy(alpha = 0.7f),
                    )
                }

                if (i < permissions.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = LocalContentColor.current.copy(alpha = 0.1f),
                    )
                }
            }
        }
    }
}
