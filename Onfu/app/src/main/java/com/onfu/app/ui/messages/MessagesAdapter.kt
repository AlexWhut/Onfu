package com.onfu.app.ui.messages

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.onfu.app.databinding.ItemMessageLeftBinding
import com.onfu.app.databinding.ItemMessageRightBinding
import com.onfu.app.domain.models.Message
import com.google.firebase.auth.FirebaseAuth

class MessagesAdapter : ListAdapter<Message, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
        }
        private const val LEFT = 0
        private const val RIGHT = 1
    }

    override fun getItemViewType(position: Int): Int {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        return if (getItem(position).senderId == uid) RIGHT else LEFT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == RIGHT) {
            RightVH(ItemMessageRightBinding.inflate(inflater, parent, false))
        } else {
            LeftVH(ItemMessageLeftBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        when (holder) {
            is RightVH -> holder.b.text.text = msg.text
            is LeftVH -> holder.b.text.text = msg.text
        }
    }

    class LeftVH(val b: ItemMessageLeftBinding) : RecyclerView.ViewHolder(b.root)
    class RightVH(val b: ItemMessageRightBinding) : RecyclerView.ViewHolder(b.root)
}
