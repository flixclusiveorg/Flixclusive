package com.flixclusive.feature.mobile.library.common.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.strings.R as LocaleR

@Composable
fun CreateLibraryDialog(
    onCreate: (String, String?) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf<String?>(null) }

    BaseLibraryModificationDialog(
        label = stringResource(LocaleR.string.new_list),
        name = name,
        description = description,
        onNameChange = { name = it },
        onDescriptionChange = { description = it },
        onConfirm = { onCreate(name, description) },
        confirmLabel = stringResource(LocaleR.string.create),
        onCancel = onCancel
    )
}
