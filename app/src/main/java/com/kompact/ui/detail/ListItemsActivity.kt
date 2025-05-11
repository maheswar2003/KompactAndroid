package com.kompact.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kompact.R
import com.kompact.data.ListItem
import com.kompact.databinding.ActivityListItemsBinding
import com.kompact.databinding.DialogAddEditItemBinding
import org.json.JSONObject

class ListItemsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListItemsBinding
    private lateinit var adapter: ListItemAdapter

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
        binding = ActivityListItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarListItems)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "$listName ($listCategory)"

        setupRecyclerView()
        observeViewModel()
        setupFab()
    }

    private fun setupRecyclerView() {
        adapter = ListItemAdapter(
            onItemClicked = { listItem -> showAddEditItemDialog(listItem) },
            onCheckboxChanged = { listItem, isChecked ->
                val newStatus = if (isChecked) "Completed" else "Pending"
                listItemViewModel.updateItemStatus(listItem, newStatus)
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
        val dialogBinding = DialogAddEditItemBinding.inflate(LayoutInflater.from(this))
        val isMovieList = listCategory == "Movies"

        dialogBinding.movieFieldsContainer.visibility = if (isMovieList) View.VISIBLE else View.GONE

        val dialogTitle =
            if (itemToEdit == null) getString(R.string.add_item) else getString(R.string.edit_item)
        val positiveButtonText =
            if (itemToEdit == null) getString(R.string.add) else getString(R.string.save)

        // Set hints from resources
        dialogBinding.editTextItemTitle.hint = getString(R.string.title_required)
        dialogBinding.editTextItemNotes.hint = getString(R.string.notes_optional)
        if (isMovieList) {
            dialogBinding.editTextDirector.hint = getString(R.string.director_optional)
            dialogBinding.editTextReleaseYear.hint = getString(R.string.release_year)
        }

        itemToEdit?.let {
            dialogBinding.editTextItemTitle.setText(it.item_title)
            dialogBinding.editTextItemNotes.setText(it.item_notes)
            if (isMovieList && !it.custom_fields.isNullOrEmpty()) {
                try {
                    val json = JSONObject(it.custom_fields)
                    dialogBinding.editTextDirector.setText(json.optString("director"))
                    dialogBinding.editTextReleaseYear.setText(json.optString("release_year"))
                } catch (e: Exception) {
                    // Log error or handle - e.g. custom_fields might be malformed
                }
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(dialogTitle)
            .setView(dialogBinding.root)
            .setPositiveButton(
                positiveButtonText,
                null
            ) // Set to null initially to prevent auto-dismiss
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .apply {
                setOnShowListener { dialog ->
                    getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val title = dialogBinding.editTextItemTitle.text.toString().trim()
                        val notes = dialogBinding.editTextItemNotes.text.toString().trim()
                            .takeIf { it.isNotEmpty() }
                        var director: String? = null
                        var releaseYearStr: String? = null
                        var releaseYear: Int? = null
                        var isValid = true

                        if (title.isEmpty()) {
                            dialogBinding.editTextItemTitle.error = getString(R.string.title_empty)
                            isValid = false
                        } else {
                            dialogBinding.editTextItemTitle.error = null
                        }

                        if (isMovieList) {
                            director = dialogBinding.editTextDirector.text.toString().trim()
                                .takeIf { it.isNotEmpty() }
                            releaseYearStr =
                                dialogBinding.editTextReleaseYear.text.toString().trim()
                            if (!releaseYearStr.isNullOrEmpty()) {
                                if (!releaseYearStr.matches("\\d{4}".toRegex())) {
                                    dialogBinding.tilReleaseYear.error =
                                        getString(R.string.invalid_year)
                                    isValid = false
                                } else {
                                    dialogBinding.tilReleaseYear.error = null
                                    releaseYear = releaseYearStr.toIntOrNull()
                                }
                            } else {
                                dialogBinding.tilReleaseYear.error = null
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
                }
            }.show()
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
} 