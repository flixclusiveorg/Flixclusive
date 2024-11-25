package com.flixclusive.feature.mobile.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis

@Composable
internal fun BaseSubScreen(
    title: String,
    description: String,
    content: LazyListScope.() -> Unit
) {
    LazyColumn {
        item {
            SubScreenHeader(
                title = title,
                description = description
            )
        }

        content.invoke(this@LazyColumn)
    }
}

@Composable
internal fun SubScreenHeader(
    title: String,
    description: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
        )

        Text(
            text = description,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
                color = LocalContentColor.current.onMediumEmphasis()
            )
        )
    }
}

@Preview
@Composable
private fun BaseSubScreenPreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            BaseSubScreen(
                title = "General",
                description = """
                    This contains all the general settings that are not related any of the specified groups on the settings navigation.
                """.trimIndent()
            ) {}
        }
    }
}