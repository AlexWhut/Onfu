package com.onfu.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.onfu.app.R
import com.onfu.app.domain.models.Post

class HomeFeedFragment : Fragment() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var adapter: HomePostsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_home_feed)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = HomePostsAdapter(
            onLikeClicked = { post -> toggleLike(post) },
            onItemClicked = { post -> openDetail(post) }
        )
        rv.adapter = adapter

        loadFeed()
    }

    private fun openDetail(post: Post) {
        val detail = com.onfu.app.ui.post.PostDetailFragment.newInstance(post.imageUrl, post.description)
        parentFragmentManager.beginTransaction()
            .replace(R.id.home_child_container, detail)
            .addToBackStack(null)
            .commit()
    }

    private fun loadFeed() {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("users").document(uid).collection("following")
            .get()
            .addOnSuccessListener { snap ->
                val followingIds = snap.documents.mapNotNull { it.getString("uid") ?: it.id }
                val owners = (followingIds + uid).distinct()
                if (owners.isEmpty()) {
                    adapter.submitList(emptyList())
                    return@addOnSuccessListener
                }

                // Simpler approach: query recent posts ordered by timestamp and filter
                // client-side by the set of owners/followed ids. This avoids multiple
                // per-owner queries and any index issues.
                firestore.collection("posts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { psnap ->
                        val allPosts = psnap.toObjects(Post::class.java)
                        val filtered = allPosts.filter { owners.contains(it.ownerId) }
                        adapter.submitList(filtered)
                        attachLikesListeners(filtered)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error loading posts: ${e.message}", Toast.LENGTH_LONG).show()
                        adapter.submitList(emptyList())
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error loading following: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun attachLikesListeners(posts: List<Post>) {
        val uid = auth.currentUser?.uid ?: return
        posts.forEach { post ->
            val postRef = firestore.collection("posts").document(post.id)
            postRef.addSnapshotListener(requireActivity()) { snap, _ ->
                if (snap == null || !snap.exists()) return@addSnapshotListener
                val currentList = adapter.currentList.toMutableList()
                val idx = currentList.indexOfFirst { it.id == post.id }
                if (idx >= 0) adapter.notifyItemChanged(idx)
            }

            postRef.collection("likes").document(uid)
                .addSnapshotListener(requireActivity()) { _, _ ->
                    val currentList = adapter.currentList.toMutableList()
                    val idx = currentList.indexOfFirst { it.id == post.id }
                    if (idx >= 0) adapter.notifyItemChanged(idx)
                }
        }
    }

    private fun toggleLike(post: Post) {
        val uid = auth.currentUser?.uid ?: return
        val postRef = firestore.collection("posts").document(post.id)
        val likeRef = postRef.collection("likes").document(uid)

        firestore.runTransaction { tx ->
            val likeSnap = tx.get(likeRef)
            val postSnap = tx.get(postRef)
            val currentCount = (postSnap.getLong("likesCount") ?: 0L)
            if (likeSnap.exists()) {
                tx.delete(likeRef)
                tx.update(postRef, "likesCount", currentCount - 1)
            } else {
                tx.set(likeRef, mapOf("uid" to uid, "ts" to System.currentTimeMillis()))
                tx.update(postRef, "likesCount", currentCount + 1)
            }
        }
            .addOnSuccessListener { }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error updating like: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
