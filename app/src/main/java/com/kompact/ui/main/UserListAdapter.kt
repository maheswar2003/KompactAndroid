package com.kompact.ui.main

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.kompact.R
import com.kompact.data.UserList
import com.kompact.data.UserListWithCount

class UserListAdapter(
    private val listener: UserListAdapterListener
) : ListAdapter<UserListWithCount, UserListAdapter.UserListViewHolder>(UserListDiffCallback()) {

    interface UserListAdapterListener {
        fun onItemClicked(userList: UserList)
        fun onItemLongClicked(userList: UserList): Boolean
    }

    private var isDragEnabled = false
    
    // Drag handle interface for external controllers
    interface OnStartDragListener {
        fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
    }
    
    private var startDragListener: OnStartDragListener? = null
    
    fun setOnStartDragListener(listener: OnStartDragListener?) {
        startDragListener = listener
    }
    
    fun setDragEnabled(enabled: Boolean) {
        if (isDragEnabled != enabled) {  // Only update if the state actually changed
            isDragEnabled = enabled
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_list, parent, false)
        return UserListViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserListViewHolder, position: Int) {
        val userListWithCount = getItem(position)
        holder.bind(userListWithCount)
    }

    inner class UserListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val textViewListName: TextView = itemView.findViewById(R.id.textViewListName)
        private val textViewItemCount: TextView = itemView.findViewById(R.id.textViewItemCount)
        private val imageViewCategoryIcon: ImageView = itemView.findViewById(R.id.imageViewCategoryIcon)
        private val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDelete)
        private val dragHandle: ImageView = itemView.findViewById(R.id.imageViewDragHandle)

        fun bind(userListWithCount: UserListWithCount) {
            val userList = userListWithCount.toUserList()
            textViewListName.text = userListWithCount.list_name
            textViewItemCount.text = userListWithCount.item_count.toString()
            
            // Set category icon based on list category type
            val iconResource = when (userListWithCount.list_category_type) {
                "Movies" -> R.drawable.ic_movie
                "Books" -> R.drawable.ic_book
                "Music" -> R.drawable.ic_music
                "Todo" -> R.drawable.ic_todo
                else -> R.drawable.ic_list
            }
            imageViewCategoryIcon.setImageResource(iconResource)
            
            // Make drag handle more prominent when drag is enabled
            if (isDragEnabled) {
                dragHandle.visibility = View.VISIBLE
                dragHandle.alpha = 1.0f
                
                // Add an elevation effect to the card to indicate it's draggable
                cardView.elevation = 8f
                
                // Make the card appear slightly different when drag is enabled
                cardView.strokeWidth = 2
                cardView.strokeColor = cardView.resources.getColor(R.color.primary, null)
            } else {
                dragHandle.visibility = View.GONE
                cardView.elevation = 2f
                cardView.strokeWidth = 0
            }
            
            // Hide checkbox and ensure card is never in checked state
            cardView.isChecked = false
            
            // Set up click listeners
            cardView.setOnClickListener {
                listener.onItemClicked(userList)
            }
            
            cardView.setOnLongClickListener {
                listener.onItemLongClicked(userList)
            }
            
            // Enhanced drag handle interaction
            dragHandle.setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        if (isDragEnabled) {
                            view.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).start()
                            startDragListener?.onStartDrag(this)
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                    }
                }
                isDragEnabled // Only consume event if drag is enabled
            }
        }
    }
}

class UserListDiffCallback : DiffUtil.ItemCallback<UserListWithCount>() {
    override fun areItemsTheSame(oldItem: UserListWithCount, newItem: UserListWithCount): Boolean {
        return oldItem.list_id == newItem.list_id
    }

    override fun areContentsTheSame(oldItem: UserListWithCount, newItem: UserListWithCount): Boolean {
        return oldItem == newItem
    }
} 