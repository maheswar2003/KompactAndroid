package com.kompact.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userList: UserList): Long

    @Update
    suspend fun update(userList: UserList)

    @Delete
    suspend fun delete(userList: UserList)

    @Query("SELECT * FROM UserLists ORDER BY creation_date DESC")
    fun getAllUserLists(): Flow<List<UserList>>

    @Query("SELECT * FROM UserLists WHERE list_id = :listId")
    fun getUserListById(listId: Long): Flow<UserList?>
} 