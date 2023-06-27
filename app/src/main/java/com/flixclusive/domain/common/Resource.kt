package com.flixclusive.domain.common


sealed class Resource<out T>(
    val data: T? = null,
    val error: String? = null,
    val isLoading: Boolean = false,
) {
    class Success<T>(_data: T) : Resource<T>(data = _data, isLoading = false)
    class Failure(_error: String) : Resource<Nothing>(error = _error, isLoading = false)
    object Loading : Resource<Nothing>(isLoading = true)
}


