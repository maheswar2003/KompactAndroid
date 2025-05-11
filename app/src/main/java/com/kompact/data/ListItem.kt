package com.kompact.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "ListItems",
    foreignKeys = [ForeignKey(
        entity = UserList::class,
        parentColumns = ["list_id"],
        childColumns = ["parent_list_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [androidx.room.Index(value = ["parent_list_id"])]
)
data class ListItem(
    @PrimaryKey(autoGenerate = true)
    val item_id: Long = 0,
    val parent_list_id: Long,
    val item_title: String,
    val item_notes: String? = null,
    var item_status: String, // e.g., 'Pending', 'Completed', or category-specific
    val creation_date: Date,
    val custom_fields: String? = null // JSON/Text blob for category-specific data
) 