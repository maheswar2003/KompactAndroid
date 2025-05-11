package com.kompact.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.kompact.data.AppDatabase
import com.kompact.data.UserList
import com.kompact.data.UserListDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class UserListViewModel(application: Application) : AndroidViewModel(application) {

    private val userListDao: UserListDao

    val allUserLists: LiveData<List<UserList>>

    init {
        val database = AppDatabase.getDatabase(application)
        userListDao = database.userListDao()
        allUserLists = userListDao.getAllUserLists().asLiveData()
    }

    fun insertList(listName: String, categoryType: String) = viewModelScope.launch(Dispatchers.IO) {
        val newList = UserList(
            list_name = listName,
            list_category_type = categoryType,
            creation_date = Date()
        )
        userListDao.insert(newList)
    }

    fun deleteList(userList: UserList) = viewModelScope.launch(Dispatchers.IO) {
        userListDao.delete(userList)
    }
} 