package com.onfu.app.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.onfu.app.R
import com.onfu.app.databinding.FragmentSearchBinding
import com.onfu.app.ui.profile.OtherProfileFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val adapter by lazy {
        SearchUserAdapter(
            onRowClick = { openProfile(it) },
            onFollowToggle = { user, currentlyFollowing ->
                toggleFollow(user.uid, !currentlyFollowing)
            }
        )
    }

    private var searchJob: Job? = null
    private var loading = false
    private var errorMessage: String? = null
    private var results: List<SearchUser> = emptyList()
    private var followingIds: Set<String> = emptySet()
    private val followLoading = mutableMapOf<String, Boolean>()
    private var currentQuery: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchResults.adapter = adapter

        binding.etSearchQuery.doAfterTextChanged { editable ->
            val text = editable?.toString() ?: ""
            currentQuery = text
            binding.btnSearchClear.visibility = if (text.isBlank()) View.GONE else View.VISIBLE
            searchJob?.cancel()
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(250)
                performSearch(text.trim())
            }
        }

        binding.btnSearchClear.setOnClickListener {
            binding.etSearchQuery.text?.clear()
        }

        updateStatusLabel()
        refreshFollowing()
    }

    private suspend fun gatherUsers(query: String): List<SearchUser> {
        if (query.length < 2) return emptyList()
        val snapshot = firestore.collection("users")
            .orderBy("userid")
            .startAt(query)
            .endAt("$query\uf8ff")
            .limit(30)
            .get()
            .await()

        val currentUid = auth.currentUser?.uid
        return snapshot.documents.mapNotNull { doc ->
            val uid = doc.getString("uid") ?: doc.id
            if (uid == currentUid) return@mapNotNull null
            val username = doc.getString("userid") ?: doc.getString("username") ?: uid
            val displayName = doc.getString("visibleName")
                ?: doc.getString("displayName")
                ?: username
            val photo = doc.getString("photoUrl")
                ?: doc.getString("avatarUrl")
                ?: ""
            val bio = doc.getString("bio") ?: ""
            val typeFlag = doc.getString("userType")?.lowercase(Locale.US) ?: ""
            val explicitVerified = doc.getBoolean("isVerified") ?: doc.getBoolean("verified") ?: false
            val isVerified = explicitVerified || typeFlag == "verified"
            SearchUser(uid, username, displayName, photo, bio, isVerified)
        }
    }

    private suspend fun performSearch(trimmed: String) {
        loading = trimmed.length >= 2
        if (!loading) {
            results = emptyList()
            errorMessage = null
            rebuildUiList()
            updateStatusLabel()
            binding.progressSearch.visibility = View.GONE
            return
        }

        errorMessage = null
        binding.progressSearch.visibility = View.VISIBLE
        try {
            results = gatherUsers(trimmed)
        } catch (t: Throwable) {
            results = emptyList()
            errorMessage = t.localizedMessage
        } finally {
            loading = false
            binding.progressSearch.visibility = View.GONE
            rebuildUiList()
            updateStatusLabel()
        }
    }

    private fun rebuildUiList() {
        val uiList = results.map { user ->
            SearchResultUi(user, followingIds.contains(user.uid), followLoading[user.uid] == true)
        }
        adapter.submitList(uiList)
    }

    private fun updateStatusLabel() {
        val trimmed = currentQuery.trim()
        val message = when {
            loading -> ""
            trimmed.isBlank() -> "Escribe al menos 2 caracteres para buscar"
            trimmed.length < 2 -> "Escribe al menos 2 caracteres para buscar"
            errorMessage != null -> "Error: ${errorMessage}"
            results.isEmpty() -> "No hay resultados"
            else -> ""
        }
        binding.tvSearchStatus.text = message
        binding.tvSearchStatus.visibility = if (message.isBlank()) View.GONE else View.VISIBLE
    }

    private fun refreshFollowing(onComplete: (() -> Unit)? = null) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            followingIds = emptySet()
            rebuildUiList()
            updateStatusLabel()
            onComplete?.invoke()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            followingIds = fetchFollowingIds(uid)
            rebuildUiList()
            updateStatusLabel()
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

    private fun toggleFollow(targetId: String, shouldFollow: Boolean, onComplete: (() -> Unit)? = null) {
        val uid = auth.currentUser?.uid ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            followLoading[targetId] = true
            rebuildUiList()
            try {
                val batch = firestore.batch()
                val followerRef = firestore.collection("users").document(targetId)
                    .collection("followers")
                    .document(uid)
                val followingRef = firestore.collection("users").document(uid)
                    .collection("following")
                    .document(targetId)
                if (shouldFollow) {
                    batch.set(followerRef, mapOf("since" to FieldValue.serverTimestamp()))
                    batch.set(followingRef, mapOf("since" to FieldValue.serverTimestamp()))
                } else {
                    batch.delete(followerRef)
                    batch.delete(followingRef)
                }
                batch.commit().await()
                followingIds = fetchFollowingIds(uid)
                rebuildUiList()
                updateStatusLabel()
                onComplete?.invoke()
            } catch (t: Throwable) {
                errorMessage = t.localizedMessage
                updateStatusLabel()
            } finally {
                followLoading[targetId] = false
                rebuildUiList()
            }
        }
    }

    private fun openProfile(user: SearchUser) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.home_child_container, OtherProfileFragment.newInstance(user.uid))
            .addToBackStack("profile_${user.uid}")
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}
