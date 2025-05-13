package com.kompact.ui.common

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

/**
 * Helper class to consistently apply window insets across the app
 */
object WindowInsetHelper {
    
    /**
     * Apply window insets to the root view of an activity
     */
    fun applySystemBarInsets(rootView: View) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val statusBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navigationBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            
            // Apply proper padding to the root view
            view.updatePadding(
                top = statusBarInsets.top,
                bottom = navigationBarInsets.bottom
            )
            
            // If root is a ViewGroup, find the toolbar/appbar and apply padding
            if (view is ViewGroup) {
                val appBarOrToolbar = findToolbarOrAppBar(view)
                appBarOrToolbar?.updateLayoutParams {
                    if (this is ViewGroup.MarginLayoutParams) {
                        this.topMargin = statusBarInsets.top
                    }
                }
            }
            
            WindowInsetsCompat.CONSUMED
        }
    }
    
    /**
     * Apply window insets to a specific content layout (usually a RecyclerView or ScrollView)
     */
    fun applyContentInsets(contentView: View) {
        ViewCompat.setOnApplyWindowInsetsListener(contentView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                bottom = insets.bottom + view.paddingBottom
            )
            WindowInsetsCompat.CONSUMED
        }
    }
    
    /**
     * Find toolbar or app bar in the view hierarchy
     */
    private fun findToolbarOrAppBar(root: ViewGroup): View? {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child.javaClass.simpleName.contains("AppBar", ignoreCase = true) ||
                child.javaClass.simpleName.contains("Toolbar", ignoreCase = true)) {
                return child
            } else if (child is ViewGroup) {
                val result = findToolbarOrAppBar(child)
                if (result != null) return result
            }
        }
        return null
    }
} 