package com.flixclusive.feature.mobile.media.component

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.domain.Async.Companion.AsyncAnimatedContent
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.core.database.entity.media.DBMedia.Companion.toDBMedia
import com.flixclusive.core.presentation.common.components.GradientCircularProgressIndicator
import com.flixclusive.core.presentation.common.extensions.buildImageRequest
import com.flixclusive.core.presentation.common.extensions.clearFocusOnSoftKeyboardHide
import com.flixclusive.core.presentation.common.extensions.toTextFieldValue
import com.flixclusive.core.presentation.common.theme.Elevations
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.Placeholder
import com.flixclusive.core.presentation.mobile.components.material3.CommonBottomSheet
import com.flixclusive.core.presentation.mobile.components.material3.CustomOutlinedTextField
import com.flixclusive.core.presentation.mobile.components.material3.topbar.ActionButton
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.theme.MobileColors.surfaceColorAtElevation
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.feature.mobile.media.LibraryListAndState
import com.flixclusive.feature.mobile.media.R
import kotlin.math.roundToInt
import kotlin.random.Random
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

private enum class ItemToggleState {
    NotAdded,
    Toggling,
    Added,
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
internal fun LibraryListSheet(
    libraryListStates: () -> Async<List<LibraryListAndState>>,
    query: () -> String,
    onQueryChange: (String) -> Unit,
    toggleOnLibrary: (String, LibraryListAndState) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val sheetState = rememberModalBottomSheetState()

    CommonBottomSheet(
        sheetState = sheetState,
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        Text(
            text = stringResource(LocaleR.string.add_to_list),
            style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 3.dp),
        )

        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.padding(vertical = 5.dp),
        )

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3F),
            modifier = Modifier
                .padding(vertical = 8.dp)
        )

        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .offset {
                            val offsetY = runCatching {
                                sheetState.requireOffset().roundToInt()
                            }.getOrDefault(0)

                            IntOffset(x = 0, y = -offsetY)
                        }
                )
            },
            modifier = Modifier
        ) {
            AsyncAnimatedContent(
                targetState = libraryListStates(),
                loadingContent = {
                    Column {
                        repeat(3) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Placeholder(
                                    elevation = Elevations.LEVEL_4,
                                    modifier = Modifier
                                        .size(40.dp)
                                )

                                Placeholder(
                                    elevation = Elevations.LEVEL_4,
                                    modifier = Modifier
                                        .height(14.dp)
                                        .width(180.dp)
                                        .padding(start = 10.dp)
                                )

                                Spacer(Modifier.weight(1f))

                                Placeholder(
                                    elevation = Elevations.LEVEL_4,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                },
                errorContent = {}
            ) { data ->
                AnimatedContent(
                    targetState = data().isEmpty(),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.fillMaxSize()
                ) { isEmpty ->
                    if (isEmpty) {
                        EmptyDataMessage()
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            items(
                                items = data(),
                                key = { it.list.id + it.providerId },
                            ) { listAndState ->
                                var buttonState by remember(listAndState.containsMedia) {
                                    mutableStateOf(
                                        if (listAndState.containsMedia) {
                                            ItemToggleState.Added
                                        } else {
                                            ItemToggleState.NotAdded
                                        }
                                    )
                                }

                                LaunchedEffect(snackbarHostState.currentSnackbarData) {
                                    if (snackbarHostState.currentSnackbarData == null) return@LaunchedEffect
                                    buttonState = if (listAndState.containsMedia) {
                                        ItemToggleState.Added
                                    } else {
                                        ItemToggleState.NotAdded
                                    }
                                }

                                ItemContent(
                                    listAndState = listAndState,
                                    toggleState = { buttonState },
                                    toggleOnLibrary = {
                                        buttonState = ItemToggleState.Toggling
                                        toggleOnLibrary(listAndState.list.id, listAndState)
                                    },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }
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

    CustomOutlinedTextField(
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
                        contentDescription = stringResource(LocaleR.string.close),
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
            .clearFocusOnSoftKeyboardHide(),
    )
}

@Composable
private fun ItemContent(
    listAndState: LibraryListAndState,
    toggleState: () -> ItemToggleState,
    toggleOnLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageSize = getAdaptiveDp(40.dp)
    val imageModel = remember(listAndState) {
        val image = listAndState.images.firstOrNull()
        context.buildImageRequest(imagePath = image)
    }

    TextButton(
        onClick = toggleOnLibrary,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    ) {
        LibraryItemIcon(
            model = imageModel,
            contentDescription = listAndState.list.name,
            modifier = Modifier
                .size(imageSize)
                .align(Alignment.CenterVertically),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(horizontal = 10.dp)
                .weight(1f)
        ) {
            Text(
                text = listAndState.list.name,
                style = LocalTextStyle.current.asAdaptiveTextStyle(),
                modifier = Modifier,
            )

            listAndState.provider?.name?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.6f),
                    modifier = Modifier,
                )
            }
        }

        AnimatedContent(
            targetState = toggleState(),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.align(Alignment.CenterVertically),
        ) { state ->
            when (state) {
                ItemToggleState.NotAdded, ItemToggleState.Added -> {
                    val isInLibrary = state == ItemToggleState.Added
                    val painter = if (isInLibrary) {
                        painterResource(R.drawable.added)
                    } else {
                        painterResource(R.drawable.add)
                    }

                    val description = if (isInLibrary) {
                        stringResource(LocaleR.string.add)
                    } else {
                        stringResource(LocaleR.string.in_library)
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

                ItemToggleState.Toggling -> {
                    GradientCircularProgressIndicator(
                        size = 20.dp,
                        thickness = 2.dp,
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryItemIcon(
    model: ImageRequest?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    var isSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(model) {
        if (model == null) {
            isSuccess = false
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3))
    ) {
        AnimatedVisibility(
            visible = !isSuccess,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Icon(
                painter = painterResource(UiCommonR.drawable.library_outline),
                contentDescription = contentDescription,
                tint = LocalContentColor.current.copy(0.8F),
                modifier = Modifier
                    .matchParentSize()
                    .scale(0.5f)
            )
        }

        AsyncImage(
            model = model,
            imageLoader = LocalContext.current.imageLoader,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            onSuccess = { isSuccess = true },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview
@Composable
private fun LibraryListSheetPreview() {
    var query by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    val lists = remember {
        val metadata = if (Random.nextBoolean()) {
            DummyDataForPreview.getMovie().toDBMedia()
        } else {
            null
        }

        val items = if (metadata != null) {
            listOf(
                LibraryListItemWithMetadata(
                    item = LibraryListItem(listId = "1", mediaId = metadata.id),
                    metadata = metadata,
                    externalIds = emptyList(),
                ),
            )
        } else {
            emptyList()
        }

        List(20) {
            LibraryListAndState(
                listWithItems = LibraryListWithItems(
                    items = items,
                    list = LibraryList(
                        id = it.toString(),
                        name = "List $it",
                        ownerId = "preview-user",
                        description = "Description $it",
                    ),
                ),
                provider = DummyDataForPreview.getProviderMetadata(
                    id = "provider-$it",
                    name = "Provider $it",
                ),
                containsMedia = Random.nextBoolean(),
            )
        }
    }

    var listState by remember {
        mutableStateOf<Async<List<LibraryListAndState>>>(Async.Success(lists))
    }

    LaunchedEffect(true) {
        // Simulate loading state
//        listState = Async.Loading
//
//        delay(2000)

        // Simulate loaded state
        listState = Async.Success(lists)

        snackbarHostState.showSnackbar("This is a snackbar message!")
    }

    FlixclusiveTheme {
        Surface {
            LibraryListSheet(
                libraryListStates = { listState },
                query = { query },
                onQueryChange = { query = it },
                toggleOnLibrary = { _, _ -> },
                onDismissRequest = {},
            )
        }
    }
}
