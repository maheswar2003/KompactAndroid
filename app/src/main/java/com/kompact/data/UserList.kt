package com.kompact.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "UserLists")
data class UserList(
    @PrimaryKey(autoGenerate = true)
    val list_id: Long = 0,
    val list_name: String,
    val list_category_type: String, // e.g., 'Generic', 'Movies', 'Books'
    val creation_date: Date
) 