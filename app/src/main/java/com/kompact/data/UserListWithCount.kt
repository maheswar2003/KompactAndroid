package com.kompact.data

import androidx.room.ColumnInfo
import java.util.Date

data class UserListWithCount(
    @ColumnInfo(name = "list_id") val list_id: Long,
    @ColumnInfo(name = "list_name") val list_name: String,
    @ColumnInfo(name = "list_category_type") val list_category_type: String,
    @ColumnInfo(name = "creation_date") val creation_date: Date,
    @ColumnInfo(name = "item_count") val item_count: Int
) {
    fun toUserList(): UserList {
        return UserList(
            list_id = list_id,
            list_name = list_name,
            list_category_type = list_category_type,
            creation_date = creation_date
        )
    }
} 