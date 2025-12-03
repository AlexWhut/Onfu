package com.onfu.app.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.onfu.app.R

class SearchUserAdapter(
    private val onRowClick: (SearchUser) -> Unit,
    private val onFollowToggle: (SearchUser, Boolean) -> Unit
) : RecyclerView.Adapter<SearchUserAdapter.VH>() {

    private val items = mutableListOf<SearchResultUi>()

    fun submitList(list: List<SearchResultUi>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_user, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.displayName.text = item.user.displayName
        holder.username.text = "@${item.user.username}"
        holder.verified.visibility = if (item.user.isVerified) View.VISIBLE else View.GONE
        holder.avatar.load(item.user.avatarUrl) {
            placeholder(android.R.drawable.sym_def_app_icon)
            error(android.R.drawable.sym_def_app_icon)
        }
        holder.followButton.isEnabled = !item.isLoading
        holder.followButton.text = when {
            item.isLoading -> holder.followButton.context.getString(R.string.loading)
            item.isFollowing -> "Siguiendo"
            else -> "Seguir"
        }

        holder.followButton.setOnClickListener {
            onFollowToggle(item.user, item.isFollowing)
        }
        holder.root.setOnClickListener { onRowClick(item.user) }
    }

    override fun getItemCount(): Int = items.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View = itemView
        val avatar: ImageView = itemView.findViewById(R.id.iv_search_avatar)
        val displayName: TextView = itemView.findViewById(R.id.tv_search_display_name)
        val username: TextView = itemView.findViewById(R.id.tv_search_username)
        val verified: ImageView = itemView.findViewById(R.id.iv_search_verified)
        val followButton: Button = itemView.findViewById(R.id.btn_search_follow)
    }
}
