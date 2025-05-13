package com.kompact.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kompact.KompactApplication
import com.kompact.R
import com.kompact.data.ListItem
import com.kompact.data.UserList
import com.kompact.databinding.ActivitySettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { exportData(it) }
    }
    
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importData(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup action bar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        // Load current theme setting
        val sharedPrefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val currentTheme = sharedPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        // Set radio button based on current theme
        when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> binding.themeRadioGroup.check(R.id.radioLightTheme)
            AppCompatDelegate.MODE_NIGHT_YES -> binding.themeRadioGroup.check(R.id.radioDarkTheme)
            else -> binding.themeRadioGroup.check(R.id.radioSystemTheme)
        }

        // Set up theme toggle listeners
        binding.themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val themeMode = when (checkedId) {
                R.id.radioLightTheme -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.radioDarkTheme -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            
            // Save theme preference
            sharedPrefs.edit().putInt("theme_mode", themeMode).apply()
            
            // Apply the theme
            AppCompatDelegate.setDefaultNightMode(themeMode)
        }
        
        // Set up data export/import buttons
        binding.buttonExportData.setOnClickListener {
            exportLauncher.launch("kompact_backup_${System.currentTimeMillis()}.json")
        }
        
        binding.buttonImportData.setOnClickListener {
            importLauncher.launch(arrayOf("application/json"))
        }
        
        // Set up Feedback option
        binding.feedbackOption.setOnClickListener {
            sendFeedbackEmail()
        }
        
        // Set up Check for Updates option
        binding.checkUpdatesOption.setOnClickListener {
            openGitHubPage()
        }
        
        // Set up animations
        setupAnimations()
    }
    
    private fun setupAnimations() {
        // Initial state - offscreen
        val itemDelay = 50L
        val duration = 300L
        
        val animatableViews = listOf(
            binding.themeTitle, 
            binding.themeRadioGroup,
            binding.dataManagementTitle,
            binding.buttonExportData,
            binding.buttonImportData,
            binding.aboutTitle,
            binding.feedbackOption,
            binding.checkUpdatesOption
        )
        
        // Set initial state
        animatableViews.forEach { view ->
            view.alpha = 0f
            view.translationY = resources.getDimension(R.dimen.item_enter_offset)
        }
        
        // Animate in sequence
        animatableViews.forEachIndexed { index, view ->
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(duration)
                .setStartDelay(100 + (index * itemDelay))
                .start()
        }
    }
    
    private fun sendFeedbackEmail() {
        // First try with ACTION_SEND which works with most email apps
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("maheswar2003@yahoo.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Feedback for Kompact App")
        }
        
        // Check if there's an app that can handle this intent
        if (emailIntent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(emailIntent, "Send feedback via:"))
        } else {
            // Fallback to mailto: URI if no email apps are installed
            val fallbackIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:maheswar2003@yahoo.com")
                putExtra(Intent.EXTRA_SUBJECT, "Feedback for Kompact App")
            }
            
            if (fallbackIntent.resolveActivity(packageManager) != null) {
                startActivity(fallbackIntent)
            } else {
                Toast.makeText(this, R.string.no_email_app, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun openGitHubPage() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/maheswar2003/KompactAndroid"))
        startActivity(intent)
    }
    
    private fun exportData(uri: Uri) {
        // Show loading indicator
        binding.buttonExportData.isEnabled = false
        binding.buttonExportData.text = getString(R.string.exporting)
        
        lifecycleScope.launch {
            try {
                // Get repository from application
                val app = application as KompactApplication
                val userListRepository = app.userListRepository
                
                // Get all data on background thread
                val data = withContext(Dispatchers.IO) {
                    // Get all lists with their items
                    val allLists = userListRepository.getAllUserLists()
                    val listsJson = JSONArray()
                    
                    for (list in allLists) {
                        val listItems = userListRepository.getListItemsForList(list.list_id)
                        
                        val listJson = JSONObject().apply {
                            put("list_id", list.list_id)
                            put("list_name", list.list_name)
                            put("list_category_type", list.list_category_type)
                            put("creation_date", list.creation_date.time)
                            
                            // Add items
                            val itemsArray = JSONArray()
                            for (item in listItems) {
                                val itemJson = JSONObject().apply {
                                    put("item_id", item.item_id)
                                    put("item_title", item.item_title)
                                    put("item_notes", item.item_notes ?: "")
                                    put("item_status", item.item_status)
                                    put("creation_date", item.creation_date.time)
                                    put("custom_fields", item.custom_fields ?: "")
                                }
                                itemsArray.put(itemJson)
                            }
                            put("items", itemsArray)
                        }
                        
                        listsJson.put(listJson)
                    }
                    
                    // Create final JSON object
                    JSONObject().apply {
                        put("exportDate", System.currentTimeMillis())
                        put("appVersion", "1.0.0")
                        put("lists", listsJson)
                    }.toString(2) // Pretty print with 2-space indentation
                }
                
                // Write to file
                withContext(Dispatchers.IO) {
                    contentResolver.openFileDescriptor(uri, "w")?.use { descriptor ->
                        FileOutputStream(descriptor.fileDescriptor).use { outputStream ->
                            outputStream.write(data.toByteArray())
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    binding.buttonExportData.isEnabled = true
                    binding.buttonExportData.text = getString(R.string.export_data)
                    Toast.makeText(this@SettingsActivity, R.string.export_success, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.buttonExportData.isEnabled = true
                    binding.buttonExportData.text = getString(R.string.export_data)
                    Toast.makeText(this@SettingsActivity, R.string.export_failed, Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun importData(uri: Uri) {
        // Show loading indicator
        binding.buttonImportData.isEnabled = false
        binding.buttonImportData.text = getString(R.string.importing)
        
        lifecycleScope.launch {
            try {
                // Read JSON from URI
                val jsonString = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            reader.readText()
                        }
                    } ?: throw Exception("Failed to open input stream")
                }
                
                val jsonData = JSONObject(jsonString)
                val listsArray = jsonData.getJSONArray("lists")
                
                // Show confirmation dialog
                withContext(Dispatchers.Main) {
                    MaterialAlertDialogBuilder(this@SettingsActivity)
                        .setTitle(R.string.confirm_import)
                        .setMessage(getString(R.string.import_count_message, listsArray.length()))
                        .setPositiveButton(R.string.import_data) { dialog, _ ->
                            dialog.dismiss()
                            // Proceed with import on background thread
                            proceedWithImport(listsArray)
                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ ->
                            binding.buttonImportData.isEnabled = true
                            binding.buttonImportData.text = getString(R.string.import_data)
                            dialog.dismiss()
                        }
                        .setCancelable(false)
                        .show()
                }
            } catch (e: Exception) {
                handleImportError(e)
            }
        }
    }
    
    private fun proceedWithImport(listsArray: JSONArray) {
        lifecycleScope.launch {
            try {
                val app = application as KompactApplication
                val userListRepository = app.userListRepository

                // Perform import on IO thread
                withContext(Dispatchers.IO) {
                    // Fetch all existing lists with their names and IDs
                    val allExistingLists = userListRepository.getAllUserLists()
                    val existingListsMapByName = allExistingLists.associateBy { it.list_name }
                    var importedListCount = 0
                    var skippedListCount = 0
                    var importedItemCount = 0
                    var skippedItemCount = 0

                    for (i in 0 until listsArray.length()) {
                        val listJson = listsArray.getJSONObject(i)
                        val listName = listJson.getString("list_name")
                        val listCategory = listJson.getString("list_category_type")
                        val listCreationDate = Date(listJson.getLong("creation_date"))
                        val itemsJsonArray = listJson.getJSONArray("items")

                        // Check if a list with the same name already exists
                        val existingList = existingListsMapByName[listName]
                        
                        // If list exists, we'll check items individually to skip duplicates
                        if (existingList != null) {
                            skippedListCount++
                            
                            // Get existing items for this list
                            val existingItems = userListRepository.getListItemsForList(existingList.list_id)
                            val existingItemsByTitle = existingItems.associateBy { it.item_title }
                            
                            // Process items for existing list
                            for (j in 0 until itemsJsonArray.length()) {
                                val itemJson = itemsJsonArray.getJSONObject(j)
                                val itemTitle = itemJson.getString("item_title")
                                
                                // Skip if item with same title already exists
                                if (existingItemsByTitle.containsKey(itemTitle)) {
                                    skippedItemCount++
                                    continue
                                }
                                
                                // Create and insert new item
                                val newListItem = ListItem(
                                    item_id = 0, // Let Room auto-generate
                                    parent_list_id = existingList.list_id,
                                    item_title = itemTitle,
                                    item_notes = if (itemJson.has("item_notes") && !itemJson.isNull("item_notes")) itemJson.getString("item_notes") else null,
                                    item_status = itemJson.getBoolean("item_status"),
                                    creation_date = Date(itemJson.getLong("creation_date")),
                                    custom_fields = if (itemJson.has("custom_fields") && !itemJson.isNull("custom_fields")) itemJson.getString("custom_fields") else null
                                )
                                userListRepository.insertListItem(newListItem)
                                importedItemCount++
                            }
                            
                            continue
                        }

                        // Create new UserList object from JSON
                        val newUserList = UserList(
                            list_id = 0, // Let Room auto-generate
                            list_name = listName,
                            list_category_type = listCategory,
                            creation_date = listCreationDate
                        )
                        // Insert the new list
                        val newListId = userListRepository.insertList(newUserList)
                        importedListCount++

                        // Import items for this list
                        for (j in 0 until itemsJsonArray.length()) {
                            val itemJson = itemsJsonArray.getJSONObject(j)
                            val newListItem = ListItem(
                                item_id = 0, // Let Room auto-generate
                                parent_list_id = newListId,
                                item_title = itemJson.getString("item_title"),
                                item_notes = if (itemJson.has("item_notes") && !itemJson.isNull("item_notes")) itemJson.getString("item_notes") else null,
                                item_status = itemJson.getBoolean("item_status"),
                                creation_date = Date(itemJson.getLong("creation_date")),
                                custom_fields = if (itemJson.has("custom_fields") && !itemJson.isNull("custom_fields")) itemJson.getString("custom_fields") else null
                            )
                            userListRepository.insertListItem(newListItem)
                            importedItemCount++
                        }
                    }

                    // Success: Update UI and set result with import stats
                    withContext(Dispatchers.Main) {
                        binding.buttonImportData.isEnabled = true
                        binding.buttonImportData.text = getString(R.string.import_data)
                        
                        // Create detailed message with counts of lists and items
                        val message = if (skippedListCount > 0 || skippedItemCount > 0) {
                            getString(R.string.import_success_detailed, 
                                     importedListCount, skippedListCount,
                                     importedItemCount, skippedItemCount)
                        } else {
                            getString(R.string.import_success_simple, 
                                     importedListCount, importedItemCount)
                        }
                        
                        Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_LONG).show()
                        // Signal MainActivity to refresh
                        setResult(RESULT_OK)
                    }
                }
            } catch (e: Exception) {
                handleImportError(e)
            }
        }
    }
    
    private fun handleImportError(e: Exception) {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.buttonImportData.isEnabled = true
            binding.buttonImportData.text = getString(R.string.import_data)
            Toast.makeText(this@SettingsActivity, R.string.import_failed, Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
} 