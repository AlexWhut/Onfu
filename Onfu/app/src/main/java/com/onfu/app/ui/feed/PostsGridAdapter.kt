package com.onfu.app.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.onfu.app.R
import com.onfu.app.domain.models.Post

class PostsGridAdapter(
    private val onClick: (Post) -> Unit
) : RecyclerView.Adapter<PostsGridAdapter.VH>() {

    private val items = mutableListOf<Post>()

    fun submitList(list: List<Post>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val post = items[position]
        // load a smaller thumbnail for grid items to speed up list scrolling
        val target = if (holder.image.width > 0) holder.image.width else (120 * holder.image.context.resources.displayMetrics.density).toInt()
        holder.image.load(post.imageUrl) {
            size(target)
            crossfade(true)
        }
        holder.caption.visibility = View.GONE
        holder.itemView.setOnClickListener { onClick(post) }
    }

    override fun getItemCount(): Int = items.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.iv_post_image)
        val caption: TextView = itemView.findViewById(R.id.post_caption)
    }
}