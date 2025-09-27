package com.flixclusive.feature.mobile.film.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.database.entity.film.DBFilm.Companion.toDBFilm
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.core.presentation.common.extensions.buildImageRequest
import com.flixclusive.core.presentation.common.extensions.clearFocusOnSoftKeyboardHide
import com.flixclusive.core.presentation.common.extensions.showSoftKeyboard
import com.flixclusive.core.presentation.common.extensions.toTextFieldValue
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.ImageWithSmallPlaceholder
import com.flixclusive.core.presentation.mobile.components.material3.topbar.ActionButton
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.feature.mobile.film.LibraryListAndState
import com.flixclusive.feature.mobile.film.R
import com.flixclusive.feature.mobile.library.common.component.CreateLibraryDialog
import kotlin.random.Random
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun LibraryListSheet(
    libraryListStates: () -> List<LibraryListAndState>,
    query: () -> String,
    onQueryChange: (String) -> Unit,
    toggleOnLibrary: (Int) -> Unit,
    createLibrary: (String, String?) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isCreateDialogOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.small.copy(
            bottomEnd = CornerSize(0.dp),
            bottomStart = CornerSize(0.dp),
        ),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(10.dp),
        ) {
            item {
                Text(
                    text = stringResource(LocaleR.string.add_to_list),
                    style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 3.dp),
                )
            }

            item {
                SearchBar(
                    query = query,
                    onQueryChange = onQueryChange,
                    modifier = Modifier.padding(vertical = 5.dp),
                )
            }

            item {
                CreateLibraryButton(
                    onClick = { isCreateDialogOpen = true },
                )
            }

            item {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3F),
                )
            }

            items(
                items = libraryListStates(),
                key = { it.list.id },
            ) { listAndState ->
                ItemContent(
                    listAndState = listAndState,
                    toggleOnLibrary = { toggleOnLibrary(listAndState.list.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }

    if (isCreateDialogOpen) {
        CreateLibraryDialog(
            onCancel = { isCreateDialogOpen = false },
            onCreate = { name, description ->
                createLibrary(name, description)
                isCreateDialogOpen = false
            },
        )
    }
}

@Composable
private fun SearchBar(
    query: () -> String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textFieldValue = remember { mutableStateOf(query().toTextFieldValue()) }

    val focusManager = LocalFocusManager.current
    val keyboardManager = LocalSoftwareKeyboardController.current

    TextField(
        value = textFieldValue.value,
        onValueChange = {
            textFieldValue.value = it
            onQueryChange(it.text)
        },
        leadingIcon = {
            AdaptiveIcon(
                painter = painterResource(UiCommonR.drawable.search_outlined),
                contentDescription = stringResource(LocaleR.string.search),
                dp = 18.dp,
                increaseBy = 3.dp,
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = textFieldValue.value.text.isNotEmpty(),
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                ActionButton(
                    onClick = {
                        textFieldValue.value = "".toTextFieldValue()
                        onQueryChange("")
                    },
                ) {
                    AdaptiveIcon(
                        painter = painterResource(UiCommonR.drawable.round_close_24),
                        contentDescription = stringResource(LocaleR.string.close_label),
                        tint = LocalContentColor.current.copy(0.6f),
                    )
                }
            }
        },
        textStyle = MaterialTheme.typography.bodyMedium.asAdaptiveTextStyle(),
        shape = MaterialTheme.shapes.small,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            focusManager.clearFocus()
            keyboardManager?.hide()
        }),
        modifier = modifier
            .fillMaxWidth()
            .showSoftKeyboard(true)
            .clearFocusOnSoftKeyboardHide(),
    )
}

@Composable
private fun CreateLibraryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    ) {
        AdaptiveIcon(
            painter = painterResource(UiCommonR.drawable.round_add_24),
            contentDescription = stringResource(LocaleR.string.new_list),
        )

        Text(
            text = stringResource(LocaleR.string.new_list),
            style = LocalTextStyle.current.asAdaptiveTextStyle(),
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
        )
    }
}

@Composable
private fun ItemContent(
    listAndState: LibraryListAndState,
    toggleOnLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageSize = getAdaptiveDp(40.dp)
    val imageModel = remember(listAndState) {
        val image = listAndState.items
            .firstOrNull()
            ?.metadata
            ?.posterImage

        context.buildImageRequest(
            imagePath = image,
            imageSize = "w45", // Small size for list icon
        )
    }

    TextButton(
        onClick = toggleOnLibrary,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    ) {
        ImageWithSmallPlaceholder(
            model = imageModel,
            contentDescription = listAndState.list.name,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(UiCommonR.drawable.library_outline),
            placeholderSize = imageSize.times(0.5f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .size(imageSize)
                .align(Alignment.CenterVertically)
        )

        Text(
            text = listAndState.list.name,
            style = LocalTextStyle.current.asAdaptiveTextStyle(),
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(horizontal = 10.dp)
                .weight(1f),
        )

        AnimatedContent(
            targetState = listAndState.containsFilm,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.align(Alignment.CenterVertically),
        ) { isInLibrary ->
            val painter = if (isInLibrary) {
                painterResource(R.drawable.added)
            } else {
                painterResource(R.drawable.add)
            }

            val description = if (isInLibrary) {
                stringResource(R.string.add)
            } else {
                stringResource(R.string.in_library)
            }

            val tint = if (isInLibrary) {
                MaterialTheme.colorScheme.primary
            } else {
                LocalContentColor.current.copy(alpha = 0.6f)
            }

            AdaptiveIcon(
                painter = painter,
                contentDescription = description,
                tint = tint,
            )
        }
    }
}

@Preview
@Composable
private fun LibraryListSheetPreview() {
    var query by remember { mutableStateOf("") }
    var lists by remember {
        val metadata = if (Random.nextBoolean()) {
            DummyDataForPreview.getMovie().toDBFilm()
        } else {
            null
        }

        val items = if (metadata != null) {
            listOf(
                LibraryListItemWithMetadata(
                    item = LibraryListItem(listId = 1, filmId = metadata.id),
                    metadata = metadata,
                ),
            )
        } else {
            emptyList()
        }

        val list = List(20) {
            LibraryListAndState(
                listWithItems = LibraryListWithItems(
                    items = items,
                    list = LibraryList(
                        id = it,
                        name = "List $it",
                        ownerId = 1,
                        description = "Description $it",
                    ),
                ),
                containsFilm = Random.nextBoolean(),
            )
        }

        mutableStateOf(list)
    }

    FlixclusiveTheme {
        Surface {
            LibraryListSheet(
                libraryListStates = { lists },
                query = { query },
                onQueryChange = { query = it },
                toggleOnLibrary = {},
                onDismissRequest = {},
                createLibrary = { name, description ->
                    lists = lists + LibraryListAndState(
                        listWithItems = LibraryListWithItems(
                            items = emptyList(),
                            list = LibraryList(
                                id = lists.size + 1,
                                name = name,
                                ownerId = 1,
                                description = description,
                            ),
                        ),
                        containsFilm = false,
                    )
                },
            )
        }
    }
}
