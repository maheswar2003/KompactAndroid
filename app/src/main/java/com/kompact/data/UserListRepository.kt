package com.kompact.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.flow.collect

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

    // Example: Insert a UserList.
    // Should be called from a coroutine or other background thread.
    suspend fun insert(userList: UserList) {
        userListDao.insert(userList)
    }

    // TODO: Add other necessary methods for UserList operations:
    // - suspend fun update(userList: UserList)
    // - suspend fun delete(userList: UserList)
    // - fun getListById(listId: Long): LiveData<UserList> 
    // etc.
} 