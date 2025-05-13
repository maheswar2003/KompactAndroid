package com.kompact.ui.detail

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.kompact.R
import com.kompact.data.ListItem
import com.kompact.databinding.ItemGenericListBinding
import org.json.JSONObject // Import for JSON handling

class ListItemAdapter(
    private val onItemClicked: (ListItem) -> Unit, // For editing
    private val onCheckboxChanged: (ListItem, Boolean) -> Unit,
    private val onDeleteClicked: (ListItem) -> Unit,
    private val onEditClicked: (ListItem) -> Unit,
    private val listCategory: String // Added listCategory
) : ListAdapter<ListItem, ListItemAdapter.ListItemViewHolder>(ListItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_list_item, parent, false)
        return ListItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ListItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val titleTextView: TextView = itemView.findViewById(R.id.textViewItemTitle)
        private val notesTextView: TextView = itemView.findViewById(R.id.textViewItemNotes)
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkboxItemStatus)
        private val editButton: ImageButton = itemView.findViewById(R.id.buttonEdit)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.buttonDelete)
        private val extraInfoView: TextView = itemView.findViewById(R.id.textViewExtraInfo)

        fun bind(item: ListItem) {
            // Set title and notes
            titleTextView.text = item.item_title
            
            if (item.item_notes.isNullOrEmpty()) {
                notesTextView.visibility = View.GONE
            } else {
                notesTextView.visibility = View.VISIBLE
                notesTextView.text = item.item_notes
            }

            // Set checkbox state
            val isCompleted = item.item_status
            checkbox.isChecked = isCompleted
            
            // Apply visual styling for completed items
            if (isCompleted) {
                // Apply strikethrough for completed items
                titleTextView.paintFlags = titleTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                notesTextView.paintFlags = notesTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                
                // Reduce opacity for completed items
                titleTextView.alpha = 0.6f
                notesTextView.alpha = 0.6f
                extraInfoView.alpha = 0.6f
                cardView.alpha = 0.8f
            } else {
                // Remove strikethrough for active items
                titleTextView.paintFlags = titleTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                notesTextView.paintFlags = notesTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                
                // Restore full opacity for active items
                titleTextView.alpha = 1.0f
                notesTextView.alpha = 1.0f
                extraInfoView.alpha = 1.0f
                cardView.alpha = 1.0f
            }

            // Handle Movies category extra fields
            if (listCategory == "Movies" && !item.custom_fields.isNullOrEmpty()) {
                try {
                    val json = JSONObject(item.custom_fields)
                    val director = json.optString("director")
                    val releaseYear = json.optString("release_year")
                    
                    val extraInfo = StringBuilder()
                    
                    if (director.isNotEmpty()) {
                        extraInfo.append(itemView.context.getString(R.string.directed_by, director))
                    }
                    
                    if (releaseYear.isNotEmpty()) {
                        if (extraInfo.isNotEmpty()) {
                            extraInfo.append(itemView.context.getString(R.string.year_dot_prefix, releaseYear))
                        } else {
                            extraInfo.append(releaseYear)
                        }
                    }
                    
                    if (extraInfo.isNotEmpty()) {
                        extraInfoView.text = extraInfo.toString()
                        extraInfoView.visibility = View.VISIBLE
                    } else {
                        extraInfoView.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    extraInfoView.visibility = View.GONE
                }
            } else {
                extraInfoView.visibility = View.GONE
            }

            // Set click listeners
            cardView.setOnClickListener { onItemClicked(item) }
            
            checkbox.setOnClickListener { 
                val newState = checkbox.isChecked
                onCheckboxChanged(item, newState)
            }
            
            editButton.setOnClickListener { onEditClicked(item) }
            deleteButton.setOnClickListener { onDeleteClicked(item) }
        }
    }
}

class ListItemDiffCallback : DiffUtil.ItemCallback<ListItem>() {
    override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
        return oldItem.item_id == newItem.item_id
    }

    override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
        return oldItem == newItem
    }
} 