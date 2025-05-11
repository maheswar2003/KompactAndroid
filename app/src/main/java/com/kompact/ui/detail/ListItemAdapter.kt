package com.kompact.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
        val binding =
            ItemGenericListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListItemViewHolder(binding, listCategory) // Pass category to ViewHolder
    }

    override fun onBindViewHolder(holder: ListItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.itemView.setOnClickListener { onItemClicked(item) } // Click on whole item for edit, or use edit button
        holder.binding.buttonEditItem.setOnClickListener { onEditClicked(item) }
        holder.binding.buttonDeleteItem.setOnClickListener { onDeleteClicked(item) }
        holder.binding.checkboxItemStatus.setOnCheckedChangeListener(null) // Avoid infinite loops/multiple calls
        holder.binding.checkboxItemStatus.isChecked = item.item_status == "Completed"
        holder.binding.checkboxItemStatus.setOnCheckedChangeListener { _, isChecked ->
            onCheckboxChanged(item, isChecked)
        }
    }

    class ListItemViewHolder(
        val binding: ItemGenericListBinding,
        private val listCategory: String
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ListItem) {
            binding.textViewItemTitle.text = item.item_title
            binding.textViewItemNotes.visibility =
                if (item.item_notes.isNullOrEmpty()) View.GONE else View.VISIBLE
            binding.textViewItemNotes.text = item.item_notes

            binding.checkboxItemStatus.isChecked = item.item_status == "Completed"

            if (listCategory == "Movies" && !item.custom_fields.isNullOrEmpty()) {
                try {
                    val json = JSONObject(item.custom_fields)
                    val director = json.optString("director", "")
                    val year = json.optString("release_year", "")
                    var subInfoText = ""
                    if (director.isNotEmpty()) subInfoText += "Directed by $director"
                    if (year.isNotEmpty()) {
                        if (subInfoText.isNotEmpty()) subInfoText += " • "
                        subInfoText += year
                    }

                    if (subInfoText.isNotEmpty()) {
                        binding.textViewSubInfo.text = subInfoText
                        binding.textViewSubInfo.visibility = View.VISIBLE
                    } else {
                        binding.textViewSubInfo.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    binding.textViewSubInfo.visibility = View.GONE
                }
            } else {
                binding.textViewSubInfo.visibility = View.GONE
            }
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