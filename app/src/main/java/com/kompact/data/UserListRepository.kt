package com.kompact.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first

// Repository class for UserList.
// This class abstracts access to multiple data sources (though currently only UserListDao).
// It provides a clean API for data access to the rest of the application.
class UserListRepository(private val userListDao: UserListDao) {

    // Example: Get all UserLists as LiveData.
    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    val allUserLists: LiveData<List<UserList>> = liveData {
        userListDao.getAllUserLists().collect { lists ->
            emit(lists)
        }
    }
    
    val allUserListsWithCount: LiveData<List<UserListWithCount>> = liveData {
        userListDao.getUserListsWithItemCount().collect { lists ->
            emit(lists)
        }
    }

    // Example: Insert a UserList.
    // Should be called from a coroutine or other background thread.
    suspend fun insert(userList: UserList) {
        userListDao.insert(userList)
    }
    
    // Insert a list and return the new ID
    suspend fun insertList(userList: UserList): Long {
        return userListDao.insertAndGetId(userList)
    }
    
    // Insert a list item and return the new ID
    suspend fun insertListItem(listItem: ListItem): Long {
        return userListDao.insertListItem(listItem)
    }
    
    suspend fun delete(userList: UserList) {
        userListDao.delete(userList)
    }
    
    suspend fun getItemCountForList(listId: Long): Int {
        return userListDao.getItemCountForList(listId)
    }
    
    // Get all lists for data export
    suspend fun getAllUserLists(): List<UserList> {
        return userListDao.getAllUserLists().first()
    }
    
    // Get all list items for a specific list
    suspend fun getListItemsForList(listId: Long): List<ListItem> {
        return userListDao.getListItemsForList(listId).first()
    }
    
    // Delete all lists (for full data replacement)
    suspend fun deleteAllLists() {
        userListDao.deleteAllLists()
    }
    
    // Delete all items for a specific list
    suspend fun deleteAllItemsForList(listId: Long) {
        userListDao.deleteAllItemsForList(listId)
    }

    // Function to update a list
    suspend fun updateList(userList: UserList) {
        userListDao.update(userList)
    }
    
    // Function to get a list by ID
    fun getListById(listId: Long): LiveData<UserList> = liveData {
        userListDao.getListById(listId).collect {
            emit(it)
        }
    }

    // Function to delete a list
    suspend fun deleteList(userList: UserList) {
        userListDao.delete(userList)
    }

    // Example of more complex operations involving multiple entities

    // TODO: Add other necessary methods for UserList operations:
    // - suspend fun update(userList: UserList)
    // - suspend fun delete(userList: UserList)
    // - fun getListById(listId: Long): LiveData<UserList> 
    // etc.
} 