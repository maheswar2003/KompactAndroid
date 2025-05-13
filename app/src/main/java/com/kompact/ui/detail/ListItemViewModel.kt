package com.kompact.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kompact.data.AppDatabase
import com.kompact.data.ListItem
import com.kompact.data.ListItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Date

class ListItemViewModel(
    application: Application,
    private val listId: Long,
    private val listCategory: String
) : AndroidViewModel(application) {

    private val repository: ListItemRepository
    val allItems: LiveData<List<ListItem>>

    init {
        val listItemDao = AppDatabase.getDatabase(application).listItemDao()
        repository = ListItemRepository(listItemDao)
        allItems = repository.getItemsByListId(listId)
    }

    fun insertItem(itemTitle: String, itemNotes: String?, director: String?, releaseYear: Int?) =
        viewModelScope.launch(Dispatchers.IO) {
            val customFieldsString = if (listCategory == "Movies") {
                JSONObject().apply {
                    put("director", director ?: "")
                    put("release_year", releaseYear?.toString() ?: "")
                }.toString()
            } else null

            val newItem = ListItem(
                parent_list_id = listId,
                item_title = itemTitle,
                item_notes = itemNotes,
                item_status = false, // Default status is false (not completed)
                creation_date = Date(),
                custom_fields = customFieldsString
            )
            repository.insert(newItem)
        }

    fun updateFullItem(
        itemToUpdate: ListItem,
        newTitle: String,
        newNotes: String?,
        newDirector: String?,
        newReleaseYear: Int?
    ) = viewModelScope.launch(Dispatchers.IO) {
        val customFieldsString = if (listCategory == "Movies") {
            JSONObject().apply {
                put("director", newDirector ?: "")
                put("release_year", newReleaseYear?.toString() ?: "")
            }.toString()
        } else null // Or preserve existing custom_fields if not a movie and it had some?

        val updatedItem = itemToUpdate.copy(
            item_title = newTitle,
            item_notes = newNotes,
            custom_fields = customFieldsString
            // status and creation_date remain unchanged from itemToUpdate
        )
        repository.update(updatedItem)
    }

    fun updateItemStatus(itemToUpdate: ListItem, newStatusIsChecked: Boolean) =
        viewModelScope.launch(Dispatchers.IO) {
            val updatedItem = itemToUpdate.copy(item_status = newStatusIsChecked)
            repository.update(updatedItem)
        }

    fun deleteItem(item: ListItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(item)
    }
}

class ListItemViewModelFactory(
    private val application: Application,
    private val listId: Long,
    private val listCategory: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ListItemViewModel(application, listId, listCategory) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 