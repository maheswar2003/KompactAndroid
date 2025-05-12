package com.kompact.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.kompact.R
import com.kompact.databinding.ActivitySettingsBinding
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader

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
    }
    
    private fun exportData(uri: Uri) {
        try {
            // In a real app, you would serialize your database data to JSON here
            val sampleData = """
                {
                  "exportDate": "${System.currentTimeMillis()}",
                  "appVersion": "1.0.0",
                  "lists": []
                }
            """.trimIndent()
            
            contentResolver.openFileDescriptor(uri, "w")?.use { descriptor ->
                FileOutputStream(descriptor.fileDescriptor).use { outputStream ->
                    outputStream.write(sampleData.toByteArray())
                }
            }
            
            Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    private fun importData(uri: Uri) {
        try {
            val stringBuilder = StringBuilder()
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            }
            
            // In a real app, you would parse the JSON and update your database
            val data = stringBuilder.toString()
            // Process data...
            
            Toast.makeText(this, R.string.import_success, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.import_failed, Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
} 