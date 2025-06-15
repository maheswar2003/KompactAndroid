package com.kompact.ui.main

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kompact.R
import com.kompact.data.UserList
import com.kompact.databinding.ActivityMainBinding
import com.kompact.ui.detail.ListItemsActivity
import com.kompact.ui.settings.SettingsActivity
import java.util.Collections

class MainActivity : AppCompatActivity(), UserListAdapter.UserListAdapterListener {

    private lateinit var binding: ActivityMainBinding
    private val userListViewModel: UserListViewModel by viewModels()
    private lateinit var adapter: UserListAdapter
    
    // Register for activity result to refresh data when returning from settings
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Data was imported, refresh UI
            userListViewModel.refreshData()
        }
    }

    private val listItemsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            userListViewModel.refreshData()
        }
    }

    private var previousSortOrder: UserListViewModel.SortOrder = UserListViewModel.SortOrder.DATE
    // Separate flags for sort order and editing mode
    private var isCustomSortActive = false 
    private var isEditModeActive = false // Flag specifically for showing the tick button

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false }
        
        super.onCreate(savedInstanceState)
        
        // Set up edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Apply the saved theme
        applyTheme()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        observeViewModel()
        
        // Check if we should enter custom sorting mode
        val currentSortOrder = userListViewModel.getSortOrder()
        if (currentSortOrder == UserListViewModel.SortOrder.CUSTOM) {
            isCustomSortActive = true
        } else {
            isCustomSortActive = false
        }
        
        // Always start with edit mode disabled
        isEditModeActive = false
        
        // Store current sort state in preferences to ensure consistency
        val sharedPrefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("is_custom_sort_active", isCustomSortActive).apply()
        
        setupFab()
        
        // Set up animation for the content
        binding.recyclerViewUserLists.alpha = 0f
        binding.recyclerViewUserLists.translationY = resources.getDimension(R.dimen.list_enter_offset)
        binding.recyclerViewUserLists.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setStartDelay(200)
            .start()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // Control visibility of menu items based on state
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val doneItem = menu.findItem(R.id.action_done_custom_sort)
        doneItem?.isVisible = isEditModeActive
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle standard menu items (Sort, Settings)
        return when (item.itemId) {
            R.id.sort_by_name -> {
                setSortOrderAndUpdate(UserListViewModel.SortOrder.NAME)
                true
            }
            R.id.sort_by_date -> {
                setSortOrderAndUpdate(UserListViewModel.SortOrder.DATE)
                true
            }
            R.id.sort_by_custom -> {
                setSortOrderAndUpdate(UserListViewModel.SortOrder.CUSTOM)
                true
            }
            R.id.action_settings -> {
                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                settingsLauncher.launch(intent)
                true
            }
            R.id.action_done_custom_sort -> {
                // Exit edit mode but keep custom sort order active
                isEditModeActive = false
                setupDragAndDrop(false)
                adapter.setDragEnabled(false)
                invalidateOptionsMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Synchronize custom sort active state with the actual sort order
        val currentSortOrder = userListViewModel.getSortOrder()
        
        // Update the sort order flag
        isCustomSortActive = currentSortOrder == UserListViewModel.SortOrder.CUSTOM
        
        // Always disable edit mode on resume
        if (isEditModeActive) {
            isEditModeActive = false
            setupDragAndDrop(false)
            adapter.setDragEnabled(false)
            invalidateOptionsMenu()
        }
    }
    
    // --- UserListAdapterListener Implementation --- 
    
    override fun onItemClicked(userList: UserList) {
        openListDetail(userList)
    }

    override fun onItemLongClicked(userList: UserList): Boolean {
        showDeleteConfirmationDialog(userList)
        return true
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

    private fun applyTheme() {
        val sharedPrefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val themeMode = sharedPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    private fun setupRecyclerView() {
        adapter = UserListAdapter(listener = this)
        binding.recyclerViewUserLists.adapter = adapter
        
        val gridLayoutManager = GridLayoutManager(this, 2)
        binding.recyclerViewUserLists.layoutManager = gridLayoutManager
        
        // Hide FAB on scroll and show "Made with Love" only when scrolled to the bottom or if content fits
        binding.recyclerViewUserLists.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // FAB handling - hide on scroll down, show on scroll up
                if (dy > 0) {
                    binding.fabAddList.hide()
                } else if (dy < 0) {
                    binding.fabAddList.show()
                }
                
                updateMadeWithLoveVisibility(recyclerView)
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updateMadeWithLoveVisibility(recyclerView)
                }
            }
        })
        
        // Check initial visibility
        binding.recyclerViewUserLists.post { updateMadeWithLoveVisibility(binding.recyclerViewUserLists) }
    }
    
    private fun updateMadeWithLoveVisibility(recyclerView: RecyclerView) {
         binding.textViewMadeWithLove.visibility = if (isScrolledToBottom(recyclerView)) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }

    private fun isScrolledToBottom(recyclerView: RecyclerView): Boolean {
        if (recyclerView.adapter == null || recyclerView.adapter?.itemCount == 0) return true
        
        val canScrollDown = recyclerView.canScrollVertically(1)
        
        if (!canScrollDown) return true

        val layoutManager = recyclerView.layoutManager as? GridLayoutManager ?: return false
        val lastVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition()
        val itemCount = recyclerView.adapter?.itemCount ?: 0
        
        return lastVisibleItemPosition == itemCount - 1
    }
    
    private fun observeViewModel() {
        userListViewModel.allUserListsWithCount.observe(this) { lists ->
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
        val categories = arrayOf("General", "Movies", "Books", "Music", "Todo", "Custom")
        
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_category_selection, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewCategories)
        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.buttonCancel)
        
        // Remove the title as it's duplicated from the dialog title
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        dialogTitle.visibility = View.GONE
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_list_category)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        val categoryAdapter = CategorySelectionAdapter(categories) { position ->
            dialog.dismiss()
            if (position == categories.size - 1) {
                // Custom category option
                showCustomCategoryDialog()
            } else {
                // Create list directly with selected category
                userListViewModel.insertList(categories[position], categories[position])
            }
        }
        
        recyclerView.adapter = categoryAdapter
        
        dialog.show()
    }

    private fun showCustomCategoryDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_category, null)
        
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextCategoryName)
        val buttonCancel = dialogView.findViewById<MaterialButton>(R.id.buttonCancel)
        val buttonContinue = dialogView.findViewById<MaterialButton>(R.id.buttonContinue)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
            
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        buttonContinue.setOnClickListener {
            val categoryName = editText.text.toString().trim()
            if (categoryName.isNotEmpty()) {
                // Create list directly with custom category name
                userListViewModel.insertList(categoryName, categoryName)
                dialog.dismiss()
            } else {
                Toast.makeText(
                    this,
                    R.string.category_name_empty,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        dialog.show()
    }

    private fun openListDetail(userList: UserList) {
        val intent = Intent(this, ListItemsActivity::class.java).apply {
            putExtra("LIST_ID", userList.list_id)
            putExtra("LIST_NAME", userList.list_name)
            putExtra("LIST_CATEGORY", userList.list_category_type)
        }
        listItemsLauncher.launch(intent)
    }

    private fun setupDragAndDrop(enabled: Boolean) {
        val recyclerView = binding.recyclerViewUserLists
        
        // First remove any existing ItemTouchHelper
        for (i in 0 until recyclerView.itemDecorationCount) {
            val itemDecoration = recyclerView.getItemDecorationAt(i)
            if (itemDecoration is ItemTouchHelper) {
                recyclerView.removeItemDecoration(itemDecoration)
            }
        }
        
        if (enabled) {
            val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN or 
                ItemTouchHelper.START or ItemTouchHelper.END,
                0
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val fromPosition = viewHolder.adapterPosition
                    val toPosition = target.adapterPosition
                    
                    // Safety check for valid positions
                    if (fromPosition == RecyclerView.NO_POSITION || 
                        toPosition == RecyclerView.NO_POSITION || 
                        adapter.currentList.isEmpty()) {
                        return false
                    }
                    
                    // Get the current list from the adapter
                    val currentList = (adapter.currentList).toMutableList()
                    
                    // Swap items in the list
                    if (fromPosition < toPosition) {
                        for (i in fromPosition until toPosition) {
                            Collections.swap(currentList, i, i + 1)
                        }
                    } else {
                        for (i in fromPosition downTo toPosition + 1) {
                            Collections.swap(currentList, i, i - 1)
                        }
                    }
                    
                    // Update the adapter
                    adapter.submitList(currentList)
                    
                    // Update the custom order in the ViewModel
                    userListViewModel.updateCustomOrder(currentList)
                    
                    return true
                }
                
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    // Not used
                }
                
                override fun isLongPressDragEnabled(): Boolean {
                    return false  // We'll manually start drag from the handle
                }
                
                // Don't use isDragEnabled() as it conflicts with the framework method
                override fun canDropOver(recyclerView: RecyclerView, current: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                    return enabled && super.canDropOver(recyclerView, current, target)
                }
            })
            
            touchHelper.attachToRecyclerView(recyclerView)
            
            // Set up drag handle listeners
            adapter.setDragEnabled(true)
            adapter.setOnStartDragListener(object : UserListAdapter.OnStartDragListener {
                override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
                    touchHelper.startDrag(viewHolder)
                }
            })
        } else {
            // Let adapter know drag is disabled
            adapter.setDragEnabled(false)
            // Remove drag listener
            adapter.setOnStartDragListener(null)
        }
    }

    private fun setSortOrderAndUpdate(sortOrder: UserListViewModel.SortOrder) {
        // Store previous sort order before updating
        previousSortOrder = userListViewModel.getSortOrder()
        userListViewModel.setSortOrder(sortOrder)

        if (sortOrder == UserListViewModel.SortOrder.CUSTOM) {
            isCustomSortActive = true
            
            // Only enter edit mode if explicitly selecting custom order from menu
            isEditModeActive = true
            
            // Apply drag functionality immediately
            setupDragAndDrop(true)
            adapter.setDragEnabled(true)
            
            // Show a brief toast explaining drag functionality
            Toast.makeText(this, R.string.custom_order_hint, Toast.LENGTH_SHORT).show()
            
            // Force a refresh to ensure list is updated
            userListViewModel.refreshData()
        } else {
            isCustomSortActive = false
            isEditModeActive = false
            setupDragAndDrop(false)
            adapter.setDragEnabled(false)
        }

        // Save the custom sort active state
        val sharedPrefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("is_custom_sort_active", isCustomSortActive).apply()

        invalidateOptionsMenu()
    }
}