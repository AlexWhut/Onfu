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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.onfu.app.R
import com.onfu.app.domain.models.Post

class HomePostsAdapter(
    private val onLikeClicked: (Post) -> Unit,
    private val onItemClicked: (Post) -> Unit
) : ListAdapter<Post, HomePostsAdapter.VH>(DIFF) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    // simple in-memory cache for user display info to avoid repeated requests
    private val userCache = mutableMapOf<String, Pair<String?, String?>>() // ownerId -> (username, photoUrl)

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
        holder.ivImage.load(post.imageUrl) { 
            crossfade(true)
            // Request original/full quality for post images so they look sharp in feed/detail
            size(coil.size.Size.ORIGINAL)
        }
        holder.tvDescription.text = post.description
        // show title if present, otherwise try cached username, otherwise ownerId fallback
        val cached = userCache[post.ownerId]
        if (!post.title.isNullOrEmpty()) {
            holder.tvUsername.text = post.title
        } else if (cached?.first != null) {
            holder.tvUsername.text = cached.first
        } else {
            holder.tvUsername.text = post.ownerId
            // fetch user doc asynchronously
            firestore.collection("users").document(post.ownerId).get()
                .addOnSuccessListener { doc ->
                    val username = doc.getString("userid") ?: doc.getString("username") ?: doc.getString("visibleName")
                    val photo = doc.getString("photoUrl")
                    userCache[post.ownerId] = Pair(username, photo)
                    // only update if this holder still represents same post owner
                    if (holder.adapterPosition == position) {
                        holder.tvUsername.text = username ?: post.ownerId
                        if (!photo.isNullOrBlank()) {
                            val t = if (holder.ivAvatar.width > 0) holder.ivAvatar.width else (40 * holder.ivAvatar.context.resources.displayMetrics.density).toInt()
                            holder.ivAvatar.load(photo) { transformations(CircleCropTransformation()); size(t); placeholder(R.drawable.logo_profile) }
                        }
                    }
                }
        }

        // load avatar if cached or fetch
        val photoUrl = cached?.second
        if (!photoUrl.isNullOrBlank()) {
            val t = if (holder.ivAvatar.width > 0) holder.ivAvatar.width else (40 * holder.ivAvatar.context.resources.displayMetrics.density).toInt()
            holder.ivAvatar.load(photoUrl) { transformations(CircleCropTransformation()); size(t); placeholder(R.drawable.logo_profile) }
        } else if (cached == null) {
            // try loading owner doc for avatar (handled above also)
            firestore.collection("users").document(post.ownerId).get()
                .addOnSuccessListener { doc ->
                    val photo = doc.getString("photoUrl")
                    if (!photo.isNullOrBlank()) holder.ivAvatar.load(photo) { transformations(CircleCropTransformation()) }
                }
        }

        holder.itemView.setOnClickListener { onItemClicked(post) }

        // populate likes count and whether current user liked
        val postRef = firestore.collection("posts").document(post.id)
        postRef.get().addOnSuccessListener { doc ->
            val count = (doc?.getLong("likesCount") ?: 0L)
            holder.tvLikes.text = count.toString()
        }

        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
            postRef.collection("likes").document(currentUid).get()
                .addOnSuccessListener { likeDoc ->
                    if (likeDoc != null && likeDoc.exists()) {
                        holder.btnLike.setImageResource(R.drawable.ic_heart_filled)
                    } else {
                        holder.btnLike.setImageResource(R.drawable.ic_heart_outline)
                    }
                }
                .addOnFailureListener {
                    holder.btnLike.setImageResource(R.drawable.ic_heart_outline)
                }
        } else {
            holder.btnLike.setImageResource(R.drawable.ic_heart_outline)
        }

        holder.btnLike.setOnClickListener {
            onLikeClicked(post)
        }
    }
}
