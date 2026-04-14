package com.flixclusive.data.database.repository
sealed class LibrarySort {
    abstract val ascending: Boolean

    data class Name(override val ascending: Boolean = false) : LibrarySort()
    data class Added(override val ascending: Boolean = false) : LibrarySort()
    data class Modified(override val ascending: Boolean = false) : LibrarySort()

    fun toggleAscending(): LibrarySort = when (this) {
        is Name -> copy(ascending = !ascending)
        is Added -> copy(ascending = !ascending)
        is Modified -> copy(ascending = !ascending)
    }
}
