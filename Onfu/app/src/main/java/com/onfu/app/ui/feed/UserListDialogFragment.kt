package com.onfu.app.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.onfu.app.R
import coil.load
import coil.transform.CircleCropTransformation
import android.util.Log
import android.widget.Toast

class UserListDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_MODE = "mode" // "followers" or "following"
        private const val ARG_USER_ID = "userId"

        fun newInstance(mode: String, userId: String): UserListDialogFragment {
            val f = UserListDialogFragment()
            val b = Bundle()
            b.putString(ARG_MODE, mode)
            b.putString(ARG_USER_ID, userId)
            f.arguments = b
            return f
        }
    }

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mode = arguments?.getString(ARG_MODE) ?: "followers"
        val userId = arguments?.getString(ARG_USER_ID) ?: return

        val rv = view.findViewById<RecyclerView>(R.id.rv_user_list)
        val title = view.findViewById<TextView>(R.id.tv_user_list_title)
        title.text = if (mode == "followers") "Followers" else "Following"

        rv.layoutManager = LinearLayoutManager(requireContext())

        // Set empty adapter immediately to avoid "No adapter attached" warnings
        rv.adapter = SimpleUserAdapter(emptyList())

        // Step 1: get the list of ids from subcollection users/{userId}/{mode}
        val colRef = firestore.collection("users").document(userId).collection(mode)

        colRef.get().addOnSuccessListener { snap ->
            val ids = snap.documents.map { it.id }
            if (ids.isEmpty()) {
                rv.adapter = SimpleUserAdapter(emptyList())
                return@addOnSuccessListener
            }

            // For each id, read user document to obtain username and photoUrl
            val users = mutableListOf<UserItem>()
            val remaining = ids.toMutableList()
            for (id in ids) {
                firestore.collection("users").document(id).get()
                    .addOnSuccessListener { doc ->
                        val username = doc.getString("visibleName") ?: doc.getString("username") ?: doc.getString("displayName") ?: doc.getString("email") ?: id
                        val photo = doc.getString("photoUrl") ?: doc.getString("avatarUrl") ?: ""
                        users.add(UserItem(id, username, photo))
                        remaining.remove(id)
                        if (remaining.isEmpty()) {
                            // sort by username
                            users.sortBy { it.displayName }
                            rv.adapter = SimpleUserAdapter(users)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w("UserListDlg", "Failed to load user $id", e)
                        remaining.remove(id)
                        if (remaining.isEmpty()) {
                            users.sortBy { it.displayName }
                            rv.adapter = SimpleUserAdapter(users)
                        }
                    }
            }
        }.addOnFailureListener { e ->
            Log.w("UserListDlg", "Failed to load $mode for $userId", e)
            Toast.makeText(requireContext(), "No permission to read $mode", Toast.LENGTH_SHORT).show()
            rv.adapter = SimpleUserAdapter(emptyList())
        }
    }

    data class UserItem(val uid: String, val displayName: String, val photoUrl: String)

    private inner class SimpleUserAdapter(private val items: List<UserItem>) : RecyclerView.Adapter<SimpleUserAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val iv: ImageView = v.findViewById(R.id.iv_user_avatar)
            val tv: TextView = v.findViewById(R.id.tv_user_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_user_row, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val it = items[position]
            holder.tv.text = it.displayName
            if (it.photoUrl.isNotEmpty()) {
                holder.iv.load(it.photoUrl) {
                    transformations(CircleCropTransformation())
                    placeholder(android.R.drawable.sym_def_app_icon)
                    error(android.R.drawable.sym_def_app_icon)
                }
            } else {
                holder.iv.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            holder.itemView.setOnClickListener {
                // TODO: navigate to profile
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
