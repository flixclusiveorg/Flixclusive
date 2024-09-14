package com.flixclusive.feature.mobile.searchExpanded.component.filter.component


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.searchExpanded.component.filter.util.toOptionString
import com.flixclusive.core.locale.R as LocaleR

/**
 *
 * Custom implementation of BottomSheet + Dialog for Compose since the vanilla one sucks ass.
 *
 * Originally from [Peter Törnhult](https://proandroiddev.com/improving-the-compose-dropdownmenu-88469b1ef34)
 * */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> SelectDropdownMenu(
    modifier: Modifier = Modifier,
    label: String?,
    options: List<T>,
    selected: Int?,
    onSelect: (Int) -> Unit,
) {
    val context = LocalContext.current
    
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = remember(selected) {
        if (selected == null)
            return@remember ""

        val option = options[selected]!!

        option.toOptionString(context = context)
    }

    Box(modifier = modifier.height(IntrinsicSize.Min)) {
        OutlinedTextField(
            label = if (label == null) null else {
                {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface.onMediumEmphasis(0.5F)
                    )
                }
            },
            value = selectedOption,
            enabled = false,
            singleLine = true,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.labelLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            ),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            onValueChange = {},
            readOnly = true,
        )

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
                .clip(MaterialTheme.shapes.extraSmall),
            color = Color.Transparent,
            onClick = { expanded = true },
            content = {}
        )
    }

    if (expanded) {
        Dialog(
            onDismissRequest = { expanded = false },
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
            ) {
                val listState = rememberLazyListState()
                if (selected != null && selected > -1) {
                    LaunchedEffect("ScrollToSelected") {
                        listState.scrollToItem(index = selected)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(10.dp),
                    state = listState
                ) {
                    itemsIndexed(options) { index, option ->
                        val selectedItem = index == selected

                        SelectDropdownMenuItem(
                            text = option.toOptionString(),
                            selected = selectedItem,
                            enabled = !selectedItem,
                            onClick = {
                                onSelect(index)
                                expanded = false
                            },
                        )

                        if (index < options.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SelectDropdownMenuItem(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.onMediumEmphasis(0.8F)
    }

    val fontWeight = when {
        selected -> FontWeight.Medium
        else -> FontWeight.Normal
    }

    Row(
        modifier = Modifier
            .clickable(enabled) { onClick() }
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            fontWeight = fontWeight,
        )

        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(LocaleR.string.check_indicator_content_desc),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}