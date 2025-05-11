package com.kompact.ui.main

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kompact.KompactApplication
import com.kompact.R
import com.kompact.data.UserList
import com.kompact.databinding.ActivityMainBinding
import com.kompact.ui.detail.ListItemsActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val userListViewModel: UserListViewModel by viewModels()
    private lateinit var adapter: UserListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeViewModel()
        setupFab()
    }

    private fun setupRecyclerView() {
        adapter = UserListAdapter(
            onListClicked = { userList ->
                openListDetail(userList)
            },
            onDeleteClicked = { userList ->
                showDeleteConfirmationDialog(userList)
            }
        )
        binding.recyclerViewUserLists.adapter = adapter
        binding.recyclerViewUserLists.layoutManager = LinearLayoutManager(this)
    }

    private fun observeViewModel() {
        userListViewModel.allUserLists.observe(this) { lists ->
            adapter.submitList(lists)
            if (lists.isNullOrEmpty()) {
                binding.textViewEmptyState.visibility = View.VISIBLE
                binding.recyclerViewUserLists.visibility = View.GONE
            } else {
                binding.textViewEmptyState.visibility = View.GONE
                binding.recyclerViewUserLists.visibility = View.VISIBLE
            }
        }
    }

    private fun setupFab() {
        binding.fabAddList.setOnClickListener {
            showCategorySelectionDialog()
        }
    }

    private fun showCategorySelectionDialog() {
        val categories = arrayOf("Generic", "Movies") // MVP categories
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_list_category)
            .setItems(categories) { dialog, which ->
                showListNameInputDialog(categories[which])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showListNameInputDialog(categoryType: String) {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            hint = getString(R.string.enter_list_name)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.create_new_list)
            .setMessage(getString(R.string.category_format, categoryType))
            .setView(editText)
            .setPositiveButton(R.string.create) { dialog, _ ->
                val listName = editText.text.toString().trim()
                if (listName.isNotEmpty()) {
                    userListViewModel.insertList(listName, categoryType)
                } else {
                    android.widget.Toast.makeText(
                        this,
                        R.string.list_name_empty,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showDeleteConfirmationDialog(userList: UserList) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_list)
            .setMessage(getString(R.string.delete_list_confirm, userList.list_name))
            .setPositiveButton(R.string.delete) { dialog, _ ->
                userListViewModel.deleteList(userList)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openListDetail(userList: UserList) {
        val intent = Intent(this, ListItemsActivity::class.java).apply {
            putExtra("LIST_ID", userList.list_id)
            putExtra("LIST_NAME", userList.list_name)
            putExtra("LIST_CATEGORY", userList.list_category_type)
        }
        startActivity(intent)
    }
} 