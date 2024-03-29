package com.flixclusive.feature.mobile.repository

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.component.provider.ProviderCard
import com.flixclusive.core.ui.mobile.component.provider.ProviderCardState
import com.flixclusive.core.ui.mobile.util.isScrollingUp
import com.flixclusive.core.ui.mobile.util.showMessage
import com.flixclusive.feature.mobile.repository.component.RepositoryHeader
import com.flixclusive.feature.mobile.repository.component.RepositoryTopBar
import com.flixclusive.gradle.entities.Author
import com.flixclusive.gradle.entities.Language
import com.flixclusive.gradle.entities.ProviderData
import com.flixclusive.gradle.entities.ProviderType
import com.flixclusive.gradle.entities.Repository
import com.flixclusive.gradle.entities.Status
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch

data class RepositoryScreenNavArgs(
    val repository: Repository
)

@Destination(
    navArgsDelegate = RepositoryScreenNavArgs::class
)
@Composable
fun RepositoryScreen(
    navigator: GoBackAction,
    args: RepositoryScreenNavArgs
) {
    val context = LocalContext.current

    // val viewModel = hiltViewModel<RepositoryScreenViewModel>()

    val listState = rememberLazyListState()
    val shouldShowTopBar by listState.isScrollingUp()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            RepositoryTopBar(
                isVisible = shouldShowTopBar,
                onNavigationIconClick = navigator::goBack
            )
        }
    ) {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(horizontal = 10.dp)
            ) {
                item {
                    RepositoryHeader(
                        repository = args.repository,
                        toggleSnackbar = {
                            scope.launch {
                                snackbarHostState.showMessage(it.asString(context))
                            }
                        }
                    )
                }

                item {
                    Divider(
                        thickness = 1.dp,
                        color = LocalContentColor.current.onMediumEmphasis(0.4F),
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                    )
                }
                
                item(2) {
                    val providerData = remember {
                        ProviderData(
                            authors = listOf(Author("FLX")),
                            repositoryUrl = null,
                            buildUrl = null,
                            changelog = null,
                            changelogMedia = null,
                            versionName = "1.0.0",
                            versionCode = 10000,
                            description = null,
                            iconUrl = null,
                            language = Language.Multiple,
                            name = "123Movies",
                            providerType = ProviderType.All,
                            status = Status.Working
                        )
                    }
                    
                    ProviderCard(
                        providerData = providerData,
                        state = ProviderCardState.Installing,
                        onClick = { /* TODO */ }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun RepositoryScreenPreview() {
    FlixclusiveTheme {
        Surface {
            RepositoryScreen(
                navigator = object : GoBackAction {
                    override fun goBack() {
                        /*TODO("Not yet implemented")*/
                    }
                },
                args = RepositoryScreenNavArgs(
                    repository = Repository(
                        "rhenwinch",
                        "Flixclusive plugins-templates",
                        "https://github.com/rhenwinch/providers",
                        ""
                    )
                )
            )
        }
    }
}