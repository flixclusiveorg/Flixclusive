package com.flixclusive.feature.mobile.user.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal sealed class AddUserState {
    data object Added : AddUserState()
    data object NotAdded : AddUserState()
}

@HiltViewModel
internal class AddUserViewModel @Inject constructor(
    homeItemsProviderUseCase: HomeItemsProviderUseCase,
    private val userRepository: UserRepository,
    private val tmdbRepository: TMDBRepository,
    private val userSessionManager: UserSessionManager
) : ViewModel() {
    private val defaultBackgrounds = listOf(
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
        viewModelScope.launch {
            with(homeItemsProviderUseCase) {
                this@with.state
                    .takeWhile { it.rowItems[1].isEmpty() }
                    .onEach {
                        val firstCatalog = it.catalogs.first()
                        val firstRowOfFilms = it.rowItems.first()

                        var backgrounds: List<String>? = null
                        if (firstRowOfFilms.isEmpty()) {
                            getCatalogItems(
                                catalog = firstCatalog, index = 0, page = 1
                            )
                        } else {
                            backgrounds = firstRowOfFilms.mapNotNull { media ->
                                media.getBestImage()
                            }.take(3)
                        }

                        if (backgrounds?.size == 3) {
                            _images.update { backgrounds }
                            cancel()
                        }
                    }
                    .catch { _images.value = defaultBackgrounds }
                    .collect()
            }
        }
    }

    private var addJob: Job? = null

    fun addUser(user: User) {
        if (addJob?.isActive == true)
            return

        addJob = viewModelScope.launch {
            userRepository.addUser(user)
            userSessionManager.signIn(user)
            _state.value = AddUserState.Added
        }
    }

    private suspend fun Film.getBestImage(): String? {
        return tmdbRepository.getPosterWithoutLogo(
            id = tmdbId!!,
            mediaType = filmType.type
        ).data ?: backdropImage
    }
}
