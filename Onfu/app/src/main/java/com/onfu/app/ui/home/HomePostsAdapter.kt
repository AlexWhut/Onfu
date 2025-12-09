package com.onfu.app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.onfu.app.R
import com.onfu.app.domain.models.Post

class HomePostsAdapter(
    private val onLikeClicked: (Post) -> Unit,
    private val onItemClicked: (Post) -> Unit
) : ListAdapter<Post, HomePostsAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem == newItem
        }
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.iv_post_image)
        val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
        val btnLike: ImageButton = itemView.findViewById(R.id.btn_like)
        val tvLikes: TextView = itemView.findViewById(R.id.tv_likes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post_home, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val post = getItem(position)
        holder.ivImage.load(post.imageUrl) { crossfade(true) }
        holder.tvDescription.text = post.description
        // username unknown here; show ownerId as fallback
        holder.tvUsername.text = post.title.ifEmpty { post.ownerId }

        holder.itemView.setOnClickListener { onItemClicked(post) }

        // likes and avatar are managed externally by fragment using live updates; default ui state shown
        holder.tvLikes.text = "0"
        holder.btnLike.setImageResource(R.drawable.ic_heart_outline)

        holder.btnLike.setOnClickListener {
            onLikeClicked(post)
        }
    }
}
