package com.flixclusive.feature.mobile.settings.screen.data

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.flixclusive.core.common.file.FileConstants
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.core.datastore.model.user.BackupOptions
import com.flixclusive.core.datastore.model.user.DataPreferences
import com.flixclusive.core.presentation.common.extensions.showToast
import com.flixclusive.core.presentation.mobile.components.material3.LabeledCheckbox
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.domain.backup.common.BackupState
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakPaddingHorizontal
import com.flixclusive.feature.mobile.settings.TweakUI
import com.flixclusive.feature.mobile.settings.component.BaseTweakDialog
import com.flixclusive.feature.mobile.settings.component.TitleDescriptionHeader
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun backupTweakGroup(
    dataPreferences: () -> DataPreferences,
    systemPreferences: () -> SystemPreferences,
    onUpdatePreferences: (suspend (DataPreferences) -> DataPreferences) -> Unit,
    onUpdateSystemPreferences: (suspend (SystemPreferences) -> SystemPreferences) -> Unit,
    createBackup: suspend (uri: Uri, options: BackupOptions) -> BackupState,
    restoreBackup: suspend (uri: Uri, options: BackupOptions) -> BackupState,
): TweakGroup {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    var isBackupOperationRunning by remember { mutableStateOf(false) }

    val frequencyOptions = persistentMapOf(
        0 to resources.getString(LocaleR.string.backup_auto_frequency_off),
        1 to resources.getString(LocaleR.string.backup_auto_frequency_every_day),
        3 to resources.getString(LocaleR.string.backup_auto_frequency_every_n_days_format, 3),
        7 to resources.getString(LocaleR.string.backup_auto_frequency_every_n_days_format, 7),
        14 to resources.getString(LocaleR.string.backup_auto_frequency_every_n_days_format, 14),
        30 to resources.getString(LocaleR.string.backup_auto_frequency_every_n_days_format, 30),
    )

    val maxBackupsOptions = persistentMapOf(
        1 to resources.getQuantityString(LocaleR.plurals.backup_count_format, 1, 1),
        3 to resources.getQuantityString(LocaleR.plurals.backup_count_format, 3, 3),
        5 to resources.getQuantityString(LocaleR.plurals.backup_count_format, 5, 5),
        10 to resources.getQuantityString(LocaleR.plurals.backup_count_format, 10, 10),
        20 to resources.getQuantityString(LocaleR.plurals.backup_count_format, 20, 20),
    )

    val selectedBackupOptions = buildSet {
        val options = dataPreferences().autoBackupOptions
        if (options.includeLibrary) add(BackupOption.LIBRARY)
        if (options.includeWatchProgress) add(BackupOption.WATCH_PROGRESS)
        if (options.includeSearchHistory) add(BackupOption.SEARCH_HISTORY)
        if (options.includePreferences) add(BackupOption.PREFERENCES)
        if (options.includeProviders) add(BackupOption.PROVIDERS)
        if (options.includeRepositories) add(BackupOption.REPOSITORIES)
    }

    val backupOptions = persistentMapOf(
        BackupOption.LIBRARY to resources.getString(LocaleR.string.backup_option_library),
        BackupOption.WATCH_PROGRESS to resources.getString(LocaleR.string.backup_option_watch_progress),
        BackupOption.SEARCH_HISTORY to resources.getString(LocaleR.string.backup_option_search_history),
        BackupOption.PREFERENCES to resources.getString(LocaleR.string.backup_option_preferences),
        BackupOption.PROVIDERS to resources.getString(LocaleR.string.backup_option_providers),
        BackupOption.REPOSITORIES to resources.getString(LocaleR.string.backup_option_repositories),
    )

    val frequencyDays = dataPreferences().autoBackupFrequencyDays
    val currentFrequencyLabel =
        when {
            frequencyDays <= 0 -> resources.getString(LocaleR.string.backup_auto_frequency_off)
            else ->
                frequencyOptions[frequencyDays]
                    ?: resources.getString(LocaleR.string.backup_auto_frequency_every_n_days_format, frequencyDays)
        }

    val maxBackups = dataPreferences().maxBackups.coerceAtLeast(1)
    val currentMaxBackupsLabel =
        maxBackupsOptions[maxBackups]
            ?: resources.getQuantityString(LocaleR.plurals.backup_count_format, maxBackups, maxBackups)

    val backupFileMimeTypes = remember {
        arrayOf(
            "application/octet-stream",
            "application/zip",
        )
    }

    val storageDirectoryUri = systemPreferences().storageDirectoryUri
        ?.takeIf { it.isNotBlank() }
        ?.let(Uri::parse)

    val storageDirectoryLabel = remember(storageDirectoryUri) {
        when {
            storageDirectoryUri == null -> resources.getString(LocaleR.string.onboarding_storage_not_selected)
            else -> DocumentFile.fromTreeUri(context, storageDirectoryUri)?.name ?: storageDirectoryUri.toString()
        }
    }

    val directoryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }

        onUpdateSystemPreferences {
            it.copy(storageDirectoryUri = uri.toString())
        }
    }

    var pendingCreateBackupOptions by remember { mutableStateOf<BackupOptions?>(null) }

    val createFileSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/*"),
    ) { uri ->
        val options = pendingCreateBackupOptions
        pendingCreateBackupOptions = null

        if (uri == null || options == null) return@rememberLauncherForActivityResult

        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, flags)
        }

        scope.launch {
            if (isBackupOperationRunning) return@launch
            isBackupOperationRunning = true

            context.showToast(resources.getString(LocaleR.string.backup_create_started))

            val state = createBackup(uri, options)

            when (state) {
                is BackupState.Success -> context.showToast(
                    resources.getString(LocaleR.string.backup_create_success),
                )

                is BackupState.Error -> context.showToast(
                    resources.getString(
                        LocaleR.string.backup_create_failed_format,
                        state.error.message ?: state.error.toString(),
                    ),
                )

                BackupState.Loading -> Unit
            }

            isBackupOperationRunning = false
        }
    }

    val restoreFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, flags)
        }

        val options = dataPreferences().autoBackupOptions
        scope.launch {
            if (isBackupOperationRunning) return@launch
            isBackupOperationRunning = true

            context.showToast(resources.getString(LocaleR.string.backup_restore_started))

            val state = restoreBackup(uri, options)

            when (state) {
                is BackupState.Success -> context.showToast(
                    resources.getString(LocaleR.string.backup_restore_success),
                )

                is BackupState.Error -> context.showToast(
                    resources.getString(
                        LocaleR.string.backup_restore_failed_format,
                        state.error.message ?: state.error.toString(),
                    ),
                )

                BackupState.Loading -> Unit
            }

            isBackupOperationRunning = false
        }
    }

    var isCreateOptionsDialogShown by rememberSaveable { mutableStateOf(false) }
    val createSelectedOptions = remember { mutableStateListOf<BackupOption>() }

    LaunchedEffect(isCreateOptionsDialogShown) {
        if (!isCreateOptionsDialogShown) return@LaunchedEffect

        createSelectedOptions.clear()
        createSelectedOptions.addAll(selectedBackupOptions)
    }

    if (isCreateOptionsDialogShown) {
        BaseTweakDialog(
            title = stringResource(LocaleR.string.backup_included_data_title),
            onDismissRequest = { isCreateOptionsDialogShown = false },
            onConfirm =
                if (createSelectedOptions.isNotEmpty()) {
                    fun() {
                        isCreateOptionsDialogShown = false
                        val options = BackupOptions(
                            includeLibrary = createSelectedOptions.contains(BackupOption.LIBRARY),
                            includeWatchProgress = createSelectedOptions.contains(BackupOption.WATCH_PROGRESS),
                            includeSearchHistory = createSelectedOptions.contains(BackupOption.SEARCH_HISTORY),
                            includePreferences = createSelectedOptions.contains(BackupOption.PREFERENCES),
                            includeProviders = createSelectedOptions.contains(BackupOption.PROVIDERS),
                            includeRepositories = createSelectedOptions.contains(BackupOption.REPOSITORIES),
                        )

                        pendingCreateBackupOptions = options
                        createFileSaver.launch(
                            "manual_flixclusive_backup_${System.currentTimeMillis()}.${FileConstants.BACKUP_FILE_EXTENSION}",
                        )
                    }
                } else {
                    null
                },
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .heightIn(max = getAdaptiveDp(400.dp, 50.dp)),
            ) {
                items(
                    items = BackupOption.entries,
                    key = { it.name },
                ) { option ->
                    val label = backupOptions[option].orEmpty()
                    val isSelected = createSelectedOptions.contains(option)

                    LabeledCheckbox(
                        checked = isSelected,
                        onCheckedChange = { isAdding ->
                            when {
                                isAdding -> createSelectedOptions.add(option)
                                else -> createSelectedOptions.remove(option)
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when {
                                        isSelected -> createSelectedOptions.remove(option)
                                        else -> createSelectedOptions.add(option)
                                    }
                                }
                                .minimumInteractiveComponentSize()
                                .padding(horizontal = getAdaptiveDp(10.dp)),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            color =
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                },
                            modifier = Modifier.padding(start = getAdaptiveDp(10.dp)),
                        )
                    }
                }
            }
        }
    }

    return TweakGroup(
        title = stringResource(LocaleR.string.backup),
        tweaks = persistentListOf(
            TweakUI.ClickableTweak(
                title = stringResource(LocaleR.string.backup_location_title),
                description = {
                    if (storageDirectoryUri == null) {
                        resources.getString(LocaleR.string.backup_location_desc)
                    } else {
                        resources.getString(
                            LocaleR.string.backup_location_desc,
                            storageDirectoryLabel,
                        )
                    }
                },
                enabledProvider = { !isBackupOperationRunning },
                onClick = { directoryPicker.launch(null) },
            ),
            TweakUI.ListTweak(
                title = stringResource(LocaleR.string.backup_auto_frequency_title),
                description = { currentFrequencyLabel },
                value = { dataPreferences().autoBackupFrequencyDays },
                options = frequencyOptions,
                enabledProvider = { !isBackupOperationRunning },
                onTweaked = { days ->
                    onUpdatePreferences { oldValue ->
                        oldValue.copy(autoBackupFrequencyDays = days)
                    }
                },
            ),
            TweakUI.ListTweak(
                title = stringResource(LocaleR.string.backup_max_backups_title),
                description = { currentMaxBackupsLabel },
                value = { dataPreferences().maxBackups.coerceAtLeast(1) },
                options = maxBackupsOptions,
                enabledProvider = { !isBackupOperationRunning },
                onTweaked = { value ->
                    onUpdatePreferences { oldValue ->
                        oldValue.copy(maxBackups = value.coerceAtLeast(1))
                    }
                },
            ),
            TweakUI.MultiSelectListTweak(
                title = stringResource(LocaleR.string.backup_included_data_title),
                description = {
                    resources.getString(
                        LocaleR.string.count_selection_format,
                        selectedBackupOptions.size,
                    )
                },
                values = selectedBackupOptions,
                options = backupOptions,
                enabledProvider = { !isBackupOperationRunning },
                onTweaked = { newValues ->
                    onUpdatePreferences { oldValue ->
                        oldValue.copy(
                            autoBackupOptions =
                                oldValue.autoBackupOptions.copy(
                                    includeLibrary = newValues.contains(BackupOption.LIBRARY),
                                    includeWatchProgress = newValues.contains(BackupOption.WATCH_PROGRESS),
                                    includeSearchHistory = newValues.contains(BackupOption.SEARCH_HISTORY),
                                    includePreferences = newValues.contains(BackupOption.PREFERENCES),
                                    includeProviders = newValues.contains(BackupOption.PROVIDERS),
                                    includeRepositories = newValues.contains(BackupOption.REPOSITORIES),
                                ),
                        )
                    }
                },
            ),
            TweakUI.Divider(),
            TweakUI.CustomContentTweak(
                title = stringResource(LocaleR.string.backup_actions_title),
                description = { resources.getString(LocaleR.string.backup_actions_desc) },
                content = {
                    BackupActionsRow(
                        enabled = !isBackupOperationRunning,
                        onCreateClick = { isCreateOptionsDialogShown = true },
                        onRestoreClick = { restoreFilePicker.launch(backupFileMimeTypes) },
                    )
                },
            ),
        ),
    )
}

private enum class BackupOption {
    LIBRARY,
    WATCH_PROGRESS,
    SEARCH_HISTORY,
    PREFERENCES,
    PROVIDERS,
    REPOSITORIES,
}

@Composable
private fun BackupActionsRow(
    enabled: Boolean,
    onCreateClick: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(LocaleR.string.backup_actions_title)
    val description = stringResource(LocaleR.string.backup_actions_desc)

    val horizontalPadding = getAdaptiveDp(TweakPaddingHorizontal * 2F)
    val verticalPadding = getAdaptiveDp(10.dp)
    val buttonMinHeight = getAdaptiveDp(50.dp)

    Column(
        verticalArrangement = Arrangement.spacedBy(getAdaptiveDp(10.dp)),
        modifier =
            modifier
                .padding(horizontal = horizontalPadding)
                .padding(vertical = verticalPadding),
    ) {
        TitleDescriptionHeader(
            title = title,
            descriptionProvider = { description },
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(getAdaptiveDp(10.dp)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                enabled = enabled,
                onClick = onCreateClick,
                shape = MaterialTheme.shapes.medium,
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = buttonMinHeight),
            ) {
                Text(text = stringResource(LocaleR.string.backup_create_button))
            }

            OutlinedButton(
                enabled = enabled,
                onClick = onRestoreClick,
                shape = MaterialTheme.shapes.medium,
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = buttonMinHeight),
            ) {
                Text(text = stringResource(LocaleR.string.backup_restore_button))
            }
        }
    }
}
