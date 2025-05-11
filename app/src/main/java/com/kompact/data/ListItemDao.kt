package com.kompact.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ListItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(listItem: ListItem): Long

    @Update
    suspend fun update(listItem: ListItem)

    @Delete
    suspend fun delete(listItem: ListItem)

    @Query("SELECT * FROM ListItems WHERE parent_list_id = :listId ORDER BY creation_date DESC")
    fun getItemsByListId(listId: Long): LiveData<List<ListItem>>

    @Query("SELECT * FROM ListItems WHERE item_id = :itemId")
    fun getItemById(itemId: Long): Flow<ListItem?>
} 