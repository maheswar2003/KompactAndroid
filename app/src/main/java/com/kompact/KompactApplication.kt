package com.kompact

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.kompact.data.AppDatabase // Assuming AppDatabase is in com.kompact.data
import com.kompact.data.UserListRepository // Assuming UserListRepository is in com.kompact.data

class KompactApplication : Application() {
    // Using by lazy so the database and repository are only created when they're needed
    // and only created once per application lifecycle.
    val userListRepository: UserListRepository by lazy {
        UserListRepository(
            AppDatabase.getDatabase(this).userListDao() // Assuming these methods exist
        )
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize theme based on saved preference
        val sharedPrefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val themeMode = sharedPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }
} 