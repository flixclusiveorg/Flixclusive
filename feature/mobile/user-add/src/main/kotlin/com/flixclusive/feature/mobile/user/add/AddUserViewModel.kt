package com.flixclusive.feature.mobile.user.add

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.database.repository.UserRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.tmdb.repository.TMDBAssetsRepository
import com.flixclusive.data.tmdb.repository.TMDBHomeCatalogRepository
import com.flixclusive.model.film.Film
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        private val userRepository: UserRepository,
        private val userSessionManager: UserSessionManager,
        private val tmdbHomeCatalogRepository: TMDBHomeCatalogRepository,
        private val tmdbAssetsRepository: TMDBAssetsRepository,
        private val appDispatchers: AppDispatchers,
    ) : ViewModel() {
        private val defaultBackgrounds by lazy {
            listOf(
                "/13bHg4hwhPqauZhxgMzCLSIAM89.jpg",
                "/JfEDrH4QObfVvFtnqzZfkUp9x4.jpg",
                "/6WmpbBcZeo6Vr9f7dMqpBobCSjR.jpg",
                "/cjrSkULmG2btwLOEvWZCeO5KRY2.jpg",
                "/bMSbEx9vXCSGN4NEktjVIEuibn2.jpg",
                "/5UhrZoYLLlbigxS578hyQn2qf9W.jpg",
            ).shuffled()
        }

        private val _state = MutableStateFlow<AddUserState>(AddUserState.NotAdded)
        val state = _state.asStateFlow()

        private val _images = MutableStateFlow(emptyList<String>())
        val images = _images.asStateFlow()

        init {
            viewModelScope.launch {
                // Get some random images from TMDB
                val page = Random.nextInt(1, 20)
                when (val response = tmdbHomeCatalogRepository.getTrending(page = page)) {
                    Resource.Loading -> {
                        Unit
                    }

                    is Resource.Failure -> {
                        _images.value = defaultBackgrounds
                    }

                    is Resource.Success<*> -> {
                        val catalogs = response.data?.results ?: emptyList()
                        if (catalogs.size < 3) {
                            _images.value = defaultBackgrounds
                            return@launch
                        }

                        val items = catalogs
                            .shuffled()
                            .take(3)
                            .mapNotNull { it.getBestImage() }

                        _images.value = items.ifEmpty { defaultBackgrounds }
                    }
                }
            }
        }

        val user = mutableStateOf(User.EMPTY)

        private var addJob: Job? = null

        fun addUser(
            user: User,
            isSigningIn: Boolean,
        ) {
            if (addJob?.isActive == true) {
                return
            }

            addJob = appDispatchers.ioScope.launch {
                val userId = userRepository.addUser(user).toInt()
                if (isSigningIn) {
                    val validatedUser = user.copy(id = userId)
                    userSessionManager.signIn(validatedUser)
                }

                _state.value = AddUserState.Added
            }
        }

        private suspend fun Film.getBestImage(): String? {
            if (tmdbId == null) {
                return backdropImage
            }

            return tmdbAssetsRepository
                .getPosterWithoutLogo(
                    id = tmdbId!!,
                    mediaType = filmType.type,
                ).data ?: backdropImage
        }
    }
