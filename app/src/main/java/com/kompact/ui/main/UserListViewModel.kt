package com.kompact.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.kompact.data.AppDatabase
import com.kompact.data.UserList
import com.kompact.data.UserListDao
import com.kompact.data.UserListRepository
import com.kompact.data.UserListWithCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Date

class UserListViewModel(application: Application) : AndroidViewModel(application) {

    enum class SortOrder {
        NAME,
        DATE,
        CUSTOM
    }

    private val userListDao: UserListDao
    private val repository: UserListRepository
    
    // Trigger for refreshing data
    private val refreshTrigger = MutableLiveData(0)
    
    // Current sort order
    private val sortOrderLiveData = MutableLiveData<SortOrder>()
    
    // Map to store custom order positions
    private val customOrderMap = MutableLiveData<Map<Long, Int>>(mapOf())
    
    // Shared preferences for saving custom order
    private val sharedPrefs = application.getSharedPreferences("list_order_prefs", Application.MODE_PRIVATE)

    val allUserLists: LiveData<List<UserList>>
    val allUserListsWithCount: LiveData<List<UserListWithCount>>

    init {
        val database = AppDatabase.getDatabase(application)
        userListDao = database.userListDao()
        repository = UserListRepository(userListDao)
        
        // Load custom order from preferences
        loadCustomOrder()
        
        // Load the saved sort order preference
        loadSortOrderPreference()
        
        // Use switchMap with the refresh trigger to reload data when needed
        allUserLists = refreshTrigger.switchMap {
            userListDao.getAllUserLists().asLiveData()
        }
        
        // Combine refresh trigger and sort order to get sorted lists
        allUserListsWithCount = refreshTrigger.switchMap { _ ->
            sortOrderLiveData.switchMap { sortOrder ->
                repository.allUserListsWithCount.map { lists ->
                    when (sortOrder) {
                        SortOrder.NAME -> lists.sortedBy { it.list_name }
                        SortOrder.DATE -> lists.sortedByDescending { it.creation_date }
                        SortOrder.CUSTOM -> {
                            val order = customOrderMap.value ?: mapOf()
                            if (order.isEmpty()) {
                                // Default to date order if no custom order saved
                                lists.sortedByDescending { it.creation_date }
                            } else {
                                lists.sortedBy { userList -> 
                                    order[userList.list_id] ?: Int.MAX_VALUE 
                                }
                            }
                        }
                        null -> lists.sortedByDescending { it.creation_date } // Handle null case
                    }
                }
            }
        }
    }
    
    /**
     * Set sort order for lists and save to preferences
     */
    fun setSortOrder(sortOrder: SortOrder) {
        sortOrderLiveData.value = sortOrder
        // Save the sort order to preferences
        sharedPrefs.edit().putString("active_sort_order", sortOrder.name).apply()
    }
    
    /**
     * Get current sort order
     */
    fun getSortOrder(): SortOrder {
        return sortOrderLiveData.value ?: SortOrder.DATE
    }
    
    /**
     * Load the saved sort order preference
     */
    private fun loadSortOrderPreference() {
        val savedSortOrder = sharedPrefs.getString("active_sort_order", SortOrder.DATE.name)
        val sortOrder = try {
            SortOrder.valueOf(savedSortOrder ?: SortOrder.DATE.name)
        } catch (e: IllegalArgumentException) {
            SortOrder.DATE // Fallback if invalid value
        }
        sortOrderLiveData.value = sortOrder
    }
    
    /**
     * Refresh data after import/export operations
     */
    fun refreshData() {
        refreshTrigger.value = refreshTrigger.value?.plus(1) ?: 1
    }
    
    /**
     * Update the custom order of lists
     */
    fun updateCustomOrder(newOrderedLists: List<UserListWithCount>) {
        val newOrderMap = newOrderedLists.mapIndexed { index, userList ->
            userList.list_id to index
        }.toMap()
        
        customOrderMap.value = newOrderMap
        saveCustomOrder(newOrderMap)
        
        // If in custom sort mode, refresh to show the new order
        if (sortOrderLiveData.value == SortOrder.CUSTOM) {
            refreshData()
        }
    }
    
    /**
     * Save custom order to preferences
     */
    private fun saveCustomOrder(orderMap: Map<Long, Int>) {
        val editor = sharedPrefs.edit()
        orderMap.forEach { (listId, position) ->
            editor.putInt(listId.toString(), position)
        }
        editor.apply()
    }
    
    /**
     * Load custom order from preferences
     */
    private fun loadCustomOrder() {
        val allPrefs = sharedPrefs.all
        val loadedMap = mutableMapOf<Long, Int>()
        
        allPrefs.forEach { (key, value) ->
            val listId = key.toLongOrNull()
            val position = value as? Int
            
            if (listId != null && position != null) {
                loadedMap[listId] = position
            }
        }
        
        customOrderMap.value = loadedMap
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
        
        // Also remove from custom order if exists
        val currentOrder = customOrderMap.value?.toMutableMap() ?: mutableMapOf()
        if (currentOrder.containsKey(userList.list_id)) {
            currentOrder.remove(userList.list_id)
            customOrderMap.postValue(currentOrder)
            saveCustomOrder(currentOrder)
        }
    }
} 