package com.kompact.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kompact.data.UserList
import com.kompact.databinding.ItemUserListBinding // Assuming ViewBinding is enabled
import java.text.SimpleDateFormat
import java.util.Locale

class UserListAdapter(
    private val onListClicked: (UserList) -> Unit,
    private val onDeleteClicked: (UserList) -> Unit
) : ListAdapter<UserList, UserListAdapter.UserListViewHolder>(UserListDiffCallback()) {

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserListViewHolder {
        val binding =
            ItemUserListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserListViewHolder, position: Int) {
        val userList = getItem(position)
        holder.bind(userList, dateFormatter)
        holder.itemView.setOnClickListener { onListClicked(userList) }
        holder.binding.buttonDeleteList.setOnClickListener { onDeleteClicked(userList) }
    }

    class UserListViewHolder(val binding: ItemUserListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(userList: UserList, dateFormatter: SimpleDateFormat) {
            binding.textViewListName.text = userList.list_name
            val categoryAndDate =
                "Category: ${userList.list_category_type} | ${dateFormatter.format(userList.creation_date)}"
            binding.textViewListCategory.text = categoryAndDate
        }
    }
}

class UserListDiffCallback : DiffUtil.ItemCallback<UserList>() {
    override fun areItemsTheSame(oldItem: UserList, newItem: UserList): Boolean {
        return oldItem.list_id == newItem.list_id
    }

    override fun areContentsTheSame(oldItem: UserList, newItem: UserList): Boolean {
        return oldItem == newItem
    }
} 