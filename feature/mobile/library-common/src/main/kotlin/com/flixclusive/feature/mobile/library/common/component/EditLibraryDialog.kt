package com.flixclusive.feature.mobile.library.common.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.strings.R as LocaleR

@Composable
fun EditLibraryDialog(
    library: LibraryList,
    onSave: (LibraryList) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf(library.name) }
    var description by remember { mutableStateOf(library.description) }

    BaseLibraryModificationDialog(
        label = stringResource(LocaleR.string.edit_library),
        name = { name },
        isEditing = true,
        description = description,
        onNameChange = { name = it },
        confirmLabel = stringResource(LocaleR.string.save),
        onDescriptionChange = { description = it },
        onConfirm = {
            val newName = name.ifEmpty { library.name }

            onSave(library.copy(name = newName, description = description))
        },
        onCancel = onCancel
    )
}
