package com.onfu.app.ui.messages

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.firestore.FirebaseFirestore
import com.onfu.app.databinding.ItemChatRowBinding

class ChatListAdapter(private val onClick: (String) -> Unit) :
    ListAdapter<ChatListItem, ChatListAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ChatListItem>() {
            override fun areItemsTheSame(oldItem: ChatListItem, newItem: ChatListItem) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ChatListItem, newItem: ChatListItem) = oldItem == newItem
        }
    }

    inner class VH(val b: ItemChatRowBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemChatRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val b = holder.b
        b.lastMessage.text = item.lastText
        FirebaseFirestore.getInstance().collection("users").document(item.otherUid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("visibleName") ?: doc.getString("userid") ?: "user"
                b.displayName.text = name
                val photo = doc.getString("photoUrl")
                if (!photo.isNullOrBlank()) {
                    val target = if (b.avatar.width > 0) b.avatar.width else (48 * b.avatar.context.resources.displayMetrics.density).toInt()
                    b.avatar.load(photo) {
                        transformations(CircleCropTransformation())
                        size(target)
                        placeholder(com.onfu.app.R.drawable.logo_profile)
                        error(com.onfu.app.R.drawable.logo_profile)
                    }
                }
            }
        b.root.setOnClickListener { onClick(item.id) }
    }
}
