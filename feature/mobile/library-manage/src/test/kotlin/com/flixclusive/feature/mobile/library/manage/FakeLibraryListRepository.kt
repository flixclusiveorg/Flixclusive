package com.flixclusive.feature.mobile.library.manage

import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import com.flixclusive.core.database.entity.DBFilm
import com.flixclusive.core.database.entity.LibraryList
import com.flixclusive.core.database.entity.LibraryListWithItems
import com.flixclusive.core.database.entity.library.LibraryListAndItemCrossRef
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListItemWithLists
import com.flixclusive.core.database.entity.user.UserWithLibraryListsAndItems
import com.flixclusive.data.library.custom.LibraryListRepository
import kotlinx.coroutines.flow.Flow
import java.util.Locale

const val MAX_SIZE_LIST = 10
const val LIBRARY_FORMAT_NAME = "Library #%d"

class FakeLibraryListRepository : LibraryListRepository {
    private val lists =
        MutableList(MAX_SIZE_LIST) {
            LibraryList(
                id = it + 1,
                ownerId = DEFAULT_OWNER_ID,
                name = String.format(Locale.getDefault(), LIBRARY_FORMAT_NAME, it),
            )
        }.toMutableStateList()

    private val listItems =
        MutableList(MAX_SIZE_LIST) {
            LibraryListItem(
                id = "$it",
                film = DBFilm(title = "Film #$it"),
            )
        }.toMutableStateList()

    override fun getLists(userId: Int): Flow<List<LibraryList>> {
        return snapshotFlow { lists.filter { it.ownerId == userId } }
    }

    override fun getList(listId: Int): Flow<LibraryList?> {
        return snapshotFlow { lists.find { it.id == listId } }
    }

    override fun getListWithItems(listId: Int): Flow<LibraryListWithItems?> {
        return snapshotFlow {
            val list = lists.find { it.id == listId }
            list?.let {
                LibraryListWithItems(
                    list = list,
                    items = listItems,
                )
            }
        }
    }

    override suspend fun insertList(list: LibraryList) {
        lists.add(list)
    }

    override suspend fun updateList(list: LibraryList) {
        val index = lists.indexOfFirst { it.id == list.id }
        lists[index] = list
    }

    override suspend fun deleteListById(listId: Int) {
        lists.removeIf { it.id == listId }
    }

    override fun getItem(itemId: String): Flow<LibraryListItem?> {
        TODO("Not yet implemented")
    }

    override suspend fun addItemToList(
        listId: Int,
        item: LibraryListItem,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun updateItem(item: LibraryListItem) {
        TODO("Not yet implemented")
    }

    override fun getItemWithLists(itemId: String): Flow<LibraryListItemWithLists?> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteItemFromList(
        listId: Int,
        itemId: String,
    ) {
        TODO("Not yet implemented")
    }

    override fun getUserWithListsAndItems(userId: Int): Flow<UserWithLibraryListsAndItems> {
        return snapshotFlow {
            UserWithLibraryListsAndItems(
                user = fakeUser,
                list =
                    lists.map { list ->
                        LibraryListWithItems(
                            list = list,
                            items = listItems,
                        )
                    },
            )
        }
    }

    override fun getCrossRef(
        listId: Int,
        itemId: String,
    ): Flow<LibraryListAndItemCrossRef?> {
        TODO("Not yet implemented")
    }
}
