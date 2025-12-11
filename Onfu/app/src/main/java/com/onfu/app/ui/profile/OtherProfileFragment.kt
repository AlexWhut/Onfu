package com.onfu.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.onfu.app.R
import com.onfu.app.databinding.FragmentOtherProfileBinding
import com.onfu.app.domain.models.Post
import com.onfu.app.ui.feed.GridSpacingItemDecoration
import com.onfu.app.ui.feed.PostsGridAdapter
import com.onfu.app.ui.messages.ChatFragment
import com.onfu.app.data.messages.MessagesRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

class OtherProfileFragment : Fragment() {

    companion object {
        private const val ARG_TARGET_UID = "arg_target_uid"

        fun newInstance(uid: String) = OtherProfileFragment().apply {
            arguments = Bundle().apply { putString(ARG_TARGET_UID, uid) }
        }
    }

    private var _binding: FragmentOtherProfileBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val targetUid by lazy { requireArguments().getString(ARG_TARGET_UID) ?: "" }
    private var postsListener: ListenerRegistration? = null
    private var followersListener: ListenerRegistration? = null
    private var followingListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null
    private val messagesRepo by lazy { MessagesRepository() }
    private val postsAdapter by lazy {
        PostsGridAdapter { post -> onPostClicked(post) }
    }
    private var followingIds: Set<String> = emptySet()
    private var followLoading = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOtherProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (targetUid.isBlank()) {
            parentFragmentManager.popBackStack()
            return
        }

        binding.rvUserPosts.layoutManager = GridLayoutManager(requireContext(), 3)
        val spacingPx = (2 * resources.displayMetrics.density).toInt()
        binding.rvUserPosts.addItemDecoration(GridSpacingItemDecoration(3, spacingPx, false))
        binding.rvUserPosts.adapter = postsAdapter

        // Tabs setup: Posts default visible
        setTabsStateOther(showPosts = true)
        binding.tabPostsOther.setOnClickListener { setTabsStateOther(true) }
        binding.tabItemsOther.setOnClickListener { setTabsStateOther(false) }

        binding.btnProfileBack.setOnClickListener { parentFragmentManager.popBackStack() }
        val isSelf = auth.currentUser?.uid == targetUid
        binding.llProfileActions.visibility = if (isSelf) View.GONE else View.VISIBLE
        binding.btnProfileFollow.visibility = if (isSelf) View.GONE else View.VISIBLE
        binding.btnProfileMessage.visibility = if (isSelf) View.GONE else View.VISIBLE
        binding.btnProfileFollow.setOnClickListener { toggleFollow(!followingIds.contains(targetUid)) }
        binding.btnProfileMessage.setOnClickListener { openMessaging() }

        observeUserDocument()
        observeCounts()
        observePosts()
        refreshFollowing()
    }

    private fun setTabsStateOther(showPosts: Boolean) {
        binding.rvUserPosts.visibility = if (showPosts) View.VISIBLE else View.GONE
        binding.tvItemsComingOther.visibility = if (showPosts) View.GONE else View.VISIBLE

        if (showPosts) {
            binding.tabPostsOther.setBackgroundColor(android.graphics.Color.parseColor("#222222"))
            binding.tabPostsOther.setTextColor(android.graphics.Color.WHITE)
            binding.tabItemsOther.setBackgroundColor(android.graphics.Color.parseColor("#DDDDDD"))
            binding.tabItemsOther.setTextColor(android.graphics.Color.BLACK)
        } else {
            binding.tabItemsOther.setBackgroundColor(android.graphics.Color.parseColor("#222222"))
            binding.tabItemsOther.setTextColor(android.graphics.Color.WHITE)
            binding.tabPostsOther.setBackgroundColor(android.graphics.Color.parseColor("#DDDDDD"))
            binding.tabPostsOther.setTextColor(android.graphics.Color.BLACK)
        }
    }

    private fun observeUserDocument() {
        userListener = firestore.collection("users").document(targetUid)
            .addSnapshotListener { doc, _ ->
                if (_binding == null || doc == null) return@addSnapshotListener
                val displayName = doc.getString("visibleName") ?: doc.getString("displayName")
                val username = doc.getString("userid") ?: doc.getString("username") ?: targetUid
                val bio = doc.getString("bio") ?: ""
                val typeFlag = doc.getString("userType")?.lowercase(Locale.US) ?: ""
                val explicitVerified = doc.getBoolean("isVerified") ?: doc.getBoolean("verified") ?: false
                val isVerified = explicitVerified || typeFlag == "verified"

                binding.profileDisplayName.text = displayName ?: username
                binding.profileUsername.text = "@${username}"
                binding.profileBio.text = if (bio.isBlank()) "Sin descripción" else bio
                binding.profileVerifiedBadge.visibility = if (isVerified) View.VISIBLE else View.GONE

                val photoUrl = doc.getString("photoUrl")
                if (!photoUrl.isNullOrBlank()) {
                    binding.profileAvatar.load(photoUrl) {
                        placeholder(android.R.drawable.sym_def_app_icon)
                        error(android.R.drawable.sym_def_app_icon)
                        transformations(CircleCropTransformation())
                    }
                }
            }
    }

    private fun observeCounts() {
        followersListener = firestore.collection("users").document(targetUid)
            .collection("followers")
            .addSnapshotListener { snap, _ ->
                if (_binding == null) return@addSnapshotListener
                binding.tvFollowersCount.text = snap?.size()?.toString() ?: "0"
            }
        followingListener = firestore.collection("users").document(targetUid)
            .collection("following")
            .addSnapshotListener { snap, _ ->
                if (_binding == null) return@addSnapshotListener
                binding.tvFollowingCount.text = snap?.size()?.toString() ?: "0"
            }
    }

    private fun observePosts() {
        postsListener = firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (_binding == null) return@addSnapshotListener
                val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()
                val ownerPosts = posts.filter { it.ownerId == targetUid }
                binding.tvPostsCount.text = ownerPosts.size.toString()
                postsAdapter.submitList(ownerPosts)
            }
    }

    private fun refreshFollowing(onComplete: (() -> Unit)? = null) {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            followingIds = emptySet()
            updateFollowButton()
            onComplete?.invoke()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            followingIds = fetchFollowingIds(currentUid)
            updateFollowButton()
            onComplete?.invoke()
        }
    }

    private suspend fun fetchFollowingIds(uid: String): Set<String> {
        return firestore.collection("users").document(uid)
            .collection("following")
            .get()
            .await()
            .documents
            .map { it.id }
            .toSet()
    }

    private fun toggleFollow(shouldFollow: Boolean) {
        val currentUid = auth.currentUser?.uid ?: return
        binding.btnProfileFollow.isEnabled = false
        followLoading = true
        updateFollowButton()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val batch = firestore.batch()
                val followerRef = firestore.collection("users").document(targetUid)
                    .collection("followers").document(currentUid)
                val followingRef = firestore.collection("users").document(currentUid)
                    .collection("following").document(targetUid)
                if (shouldFollow) {
                    batch.set(followerRef, mapOf("since" to FieldValue.serverTimestamp()))
                    batch.set(followingRef, mapOf("since" to FieldValue.serverTimestamp()))
                } else {
                    batch.delete(followerRef)
                    batch.delete(followingRef)
                }
                batch.commit().await()
                refreshFollowing()
            } catch (t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                followLoading = false
                updateFollowButton()
                binding.btnProfileFollow.isEnabled = true
            }
        }
    }

    private fun updateFollowButton() {
        val isFollowing = followingIds.contains(targetUid)
        binding.btnProfileFollow.text = when {
            followLoading -> getString(R.string.loading)
            isFollowing -> "Siguiendo"
            else -> "Seguir"
        }
        val currentUid = auth.currentUser?.uid
        binding.btnProfileFollow.isEnabled = !followLoading && !currentUid.isNullOrBlank()
    }

    private fun openMessaging() {
        val otherUid = targetUid
        val currentUid = auth.currentUser?.uid
        if (currentUid.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Inicia sesión para enviar mensajes", Toast.LENGTH_SHORT).show()
            return
        }
        messagesRepo.startConversation(otherUid) { conversationId ->
            if (conversationId == null) {
                Toast.makeText(requireContext(), "No se pudo iniciar la conversación", Toast.LENGTH_LONG).show()
                return@startConversation
            }
            selectMessagesTab()
            parentFragmentManager.beginTransaction()
                .replace(R.id.home_child_container, ChatFragment.newInstance(conversationId))
                .addToBackStack(null)
                .commit()
        }
    }

    private fun selectMessagesTab() {
        requireActivity().findViewById<View>(R.id.nav_messages)?.performClick()
    }

    private fun onPostClicked(post: Post) {
        val detail = com.onfu.app.ui.post.PostDetailFragment.newInstanceForUser(post.ownerId)
        parentFragmentManager
            .beginTransaction()
            .replace(R.id.home_child_container, detail)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        postsListener?.remove()
        followersListener?.remove()
        followingListener?.remove()
        userListener?.remove()
        _binding = null
    }
}