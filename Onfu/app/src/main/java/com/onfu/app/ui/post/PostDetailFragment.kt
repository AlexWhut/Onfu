package com.onfu.app.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.onfu.app.R
import com.onfu.app.domain.models.Post
import com.onfu.app.ui.home.HomePostsAdapter

class PostDetailFragment : Fragment() {
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var adapter: HomePostsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_post_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uid = requireArguments().getString("uid") ?: ""

        val rv = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_user_posts)
        adapter = HomePostsAdapter(
            onLikeClicked = { post -> toggleLike(post) },
            onItemClicked = { _ -> /* stay on this list view */ }
        )
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(requireContext())

        // Load recent posts and filter by owner to avoid composite index requirement
        firestore.collection("posts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener(requireActivity()) { snapshot, error ->
                if (error != null || snapshot == null) {
                    this.adapter.submitList(emptyList())
                    return@addSnapshotListener
                }
                val allPosts = snapshot.toObjects(Post::class.java)
                val ownerPosts = allPosts.filter { it.ownerId == uid }
                this.adapter.submitList(ownerPosts)
            }

        view.findViewById<View>(R.id.btn_post_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    companion object {
        fun newInstanceForUser(uid: String): PostDetailFragment {
            val f = PostDetailFragment()
            f.arguments = Bundle().apply { putString("uid", uid) }
            return f
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
    }
}
