package com.kompact.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kompact.R
import com.kompact.data.UserList
import com.kompact.data.UserListWithCount
import com.kompact.databinding.ItemUserListBinding
import java.text.SimpleDateFormat
import java.util.Locale

class UserListAdapter(
    private val onListClicked: (UserList) -> Unit,
    private val onDeleteClicked: (UserList) -> Unit
) : ListAdapter<UserListWithCount, UserListAdapter.UserListViewHolder>(UserListWithCountDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserListViewHolder {
        val binding =
            ItemUserListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserListViewHolder, position: Int) {
        val userListWithCount = getItem(position)
        holder.bind(userListWithCount)
        holder.itemView.setOnClickListener { onListClicked(userListWithCount.toUserList()) }
        // Long press for delete functionality to replace the delete button
        holder.itemView.setOnLongClickListener {
            onDeleteClicked(userListWithCount.toUserList())
            true
        }
    }

    class UserListViewHolder(val binding: ItemUserListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(userListWithCount: UserListWithCount) {
            binding.textViewListName.text = userListWithCount.list_name
            
            // Set count
            binding.itemCountTextView.text = userListWithCount.item_count.toString()
            
            // Set appropriate icon based on category
            val iconResource = when (userListWithCount.list_category_type) {
                "Movies" -> android.R.drawable.ic_media_play
                "Books" -> R.drawable.ic_book
                "Apps" -> R.drawable.ic_apps
                "Games" -> R.drawable.ic_games
                "Songs" -> R.drawable.ic_music
                "TV Shows" -> R.drawable.ic_tv
                "Podcasts" -> R.drawable.ic_podcast
                "Artists" -> R.drawable.ic_artist
                "Animes" -> R.drawable.ic_anime
                "Video Games" -> R.drawable.ic_videogame
                else -> android.R.drawable.ic_menu_agenda
            }
            binding.categoryIcon.setImageResource(iconResource)
        }
    }
}

class UserListWithCountDiffCallback : DiffUtil.ItemCallback<UserListWithCount>() {
    override fun areItemsTheSame(oldItem: UserListWithCount, newItem: UserListWithCount): Boolean {
        return oldItem.list_id == newItem.list_id
    }

    override fun areContentsTheSame(oldItem: UserListWithCount, newItem: UserListWithCount): Boolean {
        return oldItem == newItem
    }
} 