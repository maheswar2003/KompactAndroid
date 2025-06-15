package com.kompact.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kompact.R
import com.kompact.data.ListItem
import com.kompact.databinding.ActivityListItemsBinding
import com.kompact.databinding.DialogAddEditItemBinding
import com.kompact.ui.common.WindowInsetHelper
import org.json.JSONObject
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButton
import android.widget.LinearLayout

class ListItemsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListItemsBinding
    private lateinit var adapter: ListItemAdapter
    private var dataChanged = false

    private val listId: Long by lazy {
        intent.getLongExtra("LIST_ID", -1L).takeIf { it != -1L }
            ?: throw IllegalStateException("Missing or invalid LIST_ID provided to ListItemsActivity")
    }
    private val listName: String by lazy {
        intent.getStringExtra("LIST_NAME") ?: getString(R.string.list_items)
    }
    private val listCategory: String by lazy {
        intent.getStringExtra("LIST_CATEGORY") ?: "Generic"
    }

    private val listItemViewModel: ListItemViewModel by viewModels {
        ListItemViewModelFactory(application, listId, listCategory)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityListItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Apply window insets - REMOVED
        // WindowInsetHelper.applySystemBarInsets(binding.root)

        setSupportActionBar(binding.toolbarListItems)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = listName

        setupRecyclerView()
        observeViewModel()
        setupFab()
    }

    private fun setupRecyclerView() {
        adapter = ListItemAdapter(
            onItemClicked = { listItem -> showAddEditItemDialog(listItem) },
            onCheckboxChanged = { listItem, isChecked ->
                listItemViewModel.updateItemStatus(listItem, isChecked)
                dataChanged = true
            },
            onDeleteClicked = { listItem -> showDeleteItemConfirmationDialog(listItem) },
            onEditClicked = { listItem -> showAddEditItemDialog(listItem) },
            listCategory = listCategory
        )
        binding.recyclerViewListItems.adapter = adapter
        binding.recyclerViewListItems.layoutManager = LinearLayoutManager(this)
    }

    private fun observeViewModel() {
        listItemViewModel.allItems.observe(this) { items ->
            adapter.submitList(items)
            binding.textViewItemsEmptyState.visibility =
                if (items.isNullOrEmpty()) View.VISIBLE else View.GONE
            binding.recyclerViewListItems.visibility =
                if (items.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun setupFab() {
        binding.fabAddItem.setOnClickListener {
            showAddEditItemDialog(null)
        }
    }

    private fun showAddEditItemDialog(itemToEdit: ListItem?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog_add_item, null)
        
        // Find views
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val editTextItemTitle = dialogView.findViewById<TextInputEditText>(R.id.editTextItemTitle)
        val editTextItemNotes = dialogView.findViewById<TextInputEditText>(R.id.editTextItemNotes)
        val buttonCancel = dialogView.findViewById<MaterialButton>(R.id.buttonCancel)
        val buttonAdd = dialogView.findViewById<MaterialButton>(R.id.buttonAdd)
        val movieFieldsContainer = dialogView.findViewById<LinearLayout>(R.id.movieFieldsContainer)
        val editTextDirector = dialogView.findViewById<TextInputEditText>(R.id.editTextDirector)
        val editTextReleaseYear = dialogView.findViewById<TextInputEditText>(R.id.editTextReleaseYear)
        val tilReleaseYear = dialogView.findViewById<TextInputLayout>(R.id.tilReleaseYear)
        
        val isMovieList = listCategory == "Movies"
        movieFieldsContainer.visibility = if (isMovieList) View.VISIBLE else View.GONE

        // Set dialog title and button text
        dialogTitle.text = if (itemToEdit == null) getString(R.string.add_item) else getString(R.string.edit_item)
        buttonAdd.text = if (itemToEdit == null) getString(R.string.add) else getString(R.string.save)

        // Set pre-filled values if editing
        itemToEdit?.let {
            editTextItemTitle.setText(it.item_title)
            editTextItemNotes.setText(it.item_notes)
            if (isMovieList && !it.custom_fields.isNullOrEmpty()) {
                try {
                    val json = JSONObject(it.custom_fields)
                    editTextDirector.setText(json.optString("director"))
                    editTextReleaseYear.setText(json.optString("release_year"))
                } catch (e: Exception) {
                    // Log error or handle - e.g. custom_fields might be malformed
                }
            }
        }
        
        // Create the dialog
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        
        // Set button click listeners
        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        buttonAdd.setOnClickListener {
            val title = editTextItemTitle.text.toString().trim()
            val notes = editTextItemNotes.text.toString().trim()
                .takeIf { it.isNotEmpty() }
            var director: String? = null
            var releaseYear: Int? = null
            var isValid = true

            if (title.isEmpty()) {
                editTextItemTitle.error = getString(R.string.title_empty)
                isValid = false
            } else {
                editTextItemTitle.error = null
            }

            if (isMovieList) {
                director = editTextDirector.text.toString().trim()
                    .takeIf { it.isNotEmpty() }
                val releaseYearText = editTextReleaseYear.text.toString().trim()
                if (releaseYearText.isNotEmpty()) {
                    if (!releaseYearText.matches("\\d{4}".toRegex())) {
                        tilReleaseYear.error = getString(R.string.invalid_year)
                        isValid = false
                    } else {
                        tilReleaseYear.error = null
                        releaseYear = releaseYearText.toIntOrNull()
                    }
                } else {
                    tilReleaseYear.error = null
                }
            }

            if (isValid) {
                if (itemToEdit == null) {
                    listItemViewModel.insertItem(title, notes, director, releaseYear)
                } else {
                    listItemViewModel.updateFullItem(
                        itemToEdit,
                        title,
                        notes,
                        director,
                        releaseYear
                    )
                }
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }

    private fun showDeleteItemConfirmationDialog(listItem: ListItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_item)
            .setMessage(getString(R.string.delete_item_confirm, listItem.item_title))
            .setPositiveButton(R.string.delete) { dialog, _ ->
                listItemViewModel.deleteItem(listItem)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun finish() {
        if (dataChanged) {
            setResult(RESULT_OK)
        }
        super.finish()
    }
} 