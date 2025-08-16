package com.flixclusive.core.database.entity

/**
 * Represents a generic item in a list, such as a watchlist or library list.
 *
 * This interface defines the common properties that any list item should have.
 * */
interface ListItem {
    val id: Long
    val filmId: String
}
