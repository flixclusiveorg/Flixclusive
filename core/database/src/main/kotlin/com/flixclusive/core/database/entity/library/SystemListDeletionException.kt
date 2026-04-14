package com.flixclusive.core.database.entity.library

/**
 * Exception thrown when attempting to delete a system library list
 * (WATCHLIST or CONTINUE_WATCHING).
 * */
class SystemListDeletionException(listType: LibraryListType) :
    IllegalStateException("Cannot delete system list: $listType")
