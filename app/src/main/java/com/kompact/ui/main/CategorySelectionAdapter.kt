package com.kompact.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.kompact.R

class CategorySelectionAdapter(
    private val categories: Array<String>,
    private val onCategorySelected: (Int) -> Unit
) : RecyclerView.Adapter<CategorySelectionAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_selection, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position], position)
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val iconView: ImageView = itemView.findViewById(R.id.iconCategory)
        private val textView: TextView = itemView.findViewById(R.id.textViewCategory)

        fun bind(category: String, position: Int) {
            textView.text = category
            
            // Set appropriate icon based on category
            val iconResource = when (category) {
                "General" -> R.drawable.ic_list
                "Movies" -> R.drawable.ic_movie
                "Books" -> R.drawable.ic_book
                "Music" -> R.drawable.ic_music
                "Todo" -> R.drawable.ic_todo
                "Custom" -> R.drawable.ic_add_category
                else -> R.drawable.ic_list
            }
            
            iconView.setImageResource(iconResource)
            
            cardView.setOnClickListener {
                onCategorySelected(position)
            }
        }
    }
} 