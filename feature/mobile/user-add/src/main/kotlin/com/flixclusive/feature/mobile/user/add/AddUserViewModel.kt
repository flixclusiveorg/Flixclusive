package com.flixclusive.feature.mobile.user.add

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.flixclusive.core.ui.common.user.UserAvatarDefaults.AVATARS_IMAGE_COUNT
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.launchOnIO
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.data.user.UserRepository
import com.flixclusive.domain.home.HomeItemsProviderUseCase
import com.flixclusive.domain.user.UserSessionManager
import com.flixclusive.model.database.User
import com.flixclusive.model.film.Film
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

internal sealed class AddUserState {
    data object Added : AddUserState()

    data object NotAdded : AddUserState()
}

@HiltViewModel
internal class AddUserViewModel
    @Inject
    constructor(
        homeItemsProviderUseCase: HomeItemsProviderUseCase,
        private val userRepository: UserRepository,
        private val tmdbRepository: TMDBRepository,
        private val userSessionManager: UserSessionManager,
    ) : ViewModel() {
        private val defaultBackgrounds =
            listOf(
                "/13bHg4hwhPqauZhxgMzCLSIAM89.jpg",
                "/JfEDrH4QObfVvFtnqzZfkUp9x4.jpg",
                "/6WmpbBcZeo6Vr9f7dMqpBobCSjR.jpg",
                "/cjrSkULmG2btwLOEvWZCeO5KRY2.jpg",
                "/bMSbEx9vXCSGN4NEktjVIEuibn2.jpg",
                "/5UhrZoYLLlbigxS578hyQn2qf9W.jpg",
            ).shuffled()

        private val _state = MutableStateFlow<AddUserState>(AddUserState.NotAdded)
        val state = _state.asStateFlow()

        private val _images = MutableStateFlow(emptyList<String>())
        val images = _images.asStateFlow()

        init {
            launchOnIO {
                with(homeItemsProviderUseCase) {
                    this@with
                        .state
                        .mapLatest {
                            val firstCatalog = it.catalogs.firstOrNull() ?: return@mapLatest
                            val firstRowOfFilms = it.rowItems.firstOrNull() ?: return@mapLatest

                            var backgrounds: List<String>? = null
                            if (firstRowOfFilms.isEmpty()) {
                                getCatalogItems(
                                    catalog = firstCatalog,
                                    index = 0,
                                    page = 1,
                                )
                                return@mapLatest
                            }

                            backgrounds =
                                firstRowOfFilms
                                    .mapNotNull { media ->
                                        media.getBestImage()
                                    }.take(3)

                            _images.update { backgrounds }
                            cancel()
                        }
                        .collect()
                }
            }
        }

        val user =
            mutableStateOf(
                User(
                    id = 0,
                    name = "",
                    image = Random.nextInt(AVATARS_IMAGE_COUNT),
                ),
            )

        private var addJob: Job? = null

        fun addUser(
            user: User,
            isSigningIn: Boolean,
        ) {
            if (addJob?.isActive == true) {
                return
            }

            addJob =
                AppDispatchers.IO.scope.launch {
                    val userId = userRepository.addUser(user).toInt()
                    if (isSigningIn) {
                        val validatedUser = user.copy(id = userId)
                        userSessionManager.signIn(validatedUser)
                    }
                    _state.value = AddUserState.Added
                }
        }

        private suspend fun Film.getBestImage(): String? {
            return tmdbRepository
                .getPosterWithoutLogo(
                    id = tmdbId!!,
                    mediaType = filmType.type,
                ).data ?: backdropImage
        }
    }
