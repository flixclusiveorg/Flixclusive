package com.flixclusive.core.database.entity.watched

/**
 * Represents the status of a watch item.
 *
 * @property COMPLETED Indicates that the item has been fully watched.
 * @property WATCHING Indicates that the item is currently being watched.
 * @property REWATCHING Indicates that the item is being watched again.
 * */
enum class WatchStatus {
    COMPLETED,
    WATCHING,
    REWATCHING,
}
