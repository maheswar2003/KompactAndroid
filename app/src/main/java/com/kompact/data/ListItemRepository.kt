package com.kompact.data

import androidx.lifecycle.LiveData

class ListItemRepository(private val listItemDao: ListItemDao) {

    fun getItemsByListId(listId: Long): LiveData<List<ListItem>> {
        return listItemDao.getItemsByListId(listId)
    }

    suspend fun insert(item: ListItem) {
        listItemDao.insert(item)
    }

    suspend fun update(item: ListItem) {
        listItemDao.update(item)
    }

    suspend fun delete(item: ListItem) {
        listItemDao.delete(item)
    }
} 