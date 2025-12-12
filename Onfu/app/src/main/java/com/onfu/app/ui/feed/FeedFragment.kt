package com.onfu.app.ui.feed

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.widget.ImageView
import android.widget.TextView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast
import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.fragment.app.Fragment
// No changes made to import statements
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.onfu.app.R
import com.onfu.app.databinding.FragmentFeedBinding
import com.onfu.app.domain.models.Post
import com.onfu.app.ui.fragments.LoginFragment
import com.onfu.app.ui.feed.GridSpacingItemDecoration


/**
 * Fragment que muestra el feed: stories arriba, RecyclerView de posts y NAV abajo.
 * Esta implementación inicial infla `fragment_feed.xml`, carga datos de ejemplo y
 * configura un adapter simple para `item_post.xml`.
 *
 * Reemplaza los placeholders por tu cargador de imágenes (Glide/Coil/Picasso) y
 * conecta la navegación en `onPostClicked` para abrir el detalle del post.
 */

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private var editMode: Boolean = false
    private var editsMade: Boolean = false

    // Avatar change helpers
    private var selectedAvatarUri: Uri? = null
    private lateinit var pickAvatarLauncher: ActivityResultLauncher<String>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize activity result launchers here to ensure Fragment is attached
        pickAvatarLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedAvatarUri = it
                _binding?.profileAvatar?.load(it) {
                    transformations(CircleCropTransformation())
                    // use LogoProfile if available, fallback to tempologo
                    val logoId = resources.getIdentifier("logo_profile", "drawable", requireContext().packageName)
                    val placeholderId = if (logoId != 0) logoId else R.drawable.tempologo
                    placeholder(placeholderId)
                    error(placeholderId)
                }
                uploadAvatarUri(it)
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pickAvatarLauncher.launch("image/*")
            } else {
                Toast.makeText(requireContext(), "Permiso denegado: no se puede acceder a las fotos", Toast.LENGTH_LONG).show()
            }
        }
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val postsAdapter by lazy {
        PostsGridAdapter { post -> onPostClicked(post) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Start of Changes ---

        // 1. Declare auth and firestore instances once at the top
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val currentUid = auth.currentUser?.uid

        // Setup RecyclerView
        val rv = binding.rvFeed
        val spanCount = 3
        val spacingDp = 2
        val spacingPx = (spacingDp * resources.displayMetrics.density).toInt()
        rv.layoutManager = GridLayoutManager(requireContext(), spanCount)
        rv.addItemDecoration(GridSpacingItemDecoration(spanCount, spacingPx, false))

        // Tabs behavior: default to Posts
        setTabsState(showPosts = true)
        binding.tabPosts.setOnClickListener {
            setTabsState(showPosts = true)
        }
        binding.tabItems.setOnClickListener {
            setTabsState(showPosts = false)
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val fm = requireActivity().supportFragmentManager
            fm.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

            // Defensive: ensure the activity actually contains the expected container before replacing.
            val containerView = requireActivity().findViewById<View?>(R.id.fragment_container)
            if (containerView != null) {
                fm.beginTransaction()
                    .replace(R.id.fragment_container, LoginFragment())
                    .commitAllowingStateLoss()
            } else {
                // Fallback: replace the activity content root so we still return to a clean LoginFragment
                fm.beginTransaction()
                    .replace(android.R.id.content, LoginFragment())
                    .commitAllowingStateLoss()
            }
        }

        // Edit profile button: toggle edit mode UI
        binding.btnEditProfile.setOnClickListener {
            if (!editMode) {
                enterEditMode()
            } else {
                // Always allow returning even if no edits were made
                exitEditMode()
            }
        }

        // Load posts ordered by time and filter client-side to only the current user's posts.
        // This matches the previous working approach and avoids index/permission issues.
        binding.rvFeed.adapter = postsAdapter
        firestore.collection("posts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener(requireActivity()) { snapshot, error ->
                if (_binding == null) return@addSnapshotListener
                if (error != null || snapshot == null) {
                    postsAdapter.submitList(emptyList())
                    return@addSnapshotListener
                }
                val posts = snapshot.toObjects(Post::class.java)
                val filtered = currentUid?.let { owner -> posts.filter { it.ownerId == owner } } ?: emptyList()
                postsAdapter.submitList(filtered)
            }

        // Live update counts and profile header logic
        if (currentUid != null) {
            // Posts count
            firestore.collection("posts").whereEqualTo("ownerId", currentUid)
                .addSnapshotListener(requireActivity()) { snap, e ->
                    if (_binding == null) return@addSnapshotListener
                    if (e != null) {
                        // permission denied or other error
                        binding.tvPostsCount.text = "0"
                    } else {
                        binding.tvPostsCount.text = (snap?.size() ?: 0).toString()
                    }
                }

            // Followers count
            firestore.collection("users").document(currentUid).collection("followers")
                .addSnapshotListener(requireActivity()) { snap, e ->
                    if (_binding == null) return@addSnapshotListener
                    if (e != null) {
                        binding.tvFollowersCount.text = "0"
                    } else {
                        binding.tvFollowersCount.text = (snap?.size() ?: 0).toString()
                    }
                }

            // Following count
            firestore.collection("users").document(currentUid).collection("following")
                .addSnapshotListener(requireActivity()) { snap, e ->
                    if (_binding == null) return@addSnapshotListener
                    if (e != null) {
                        binding.tvFollowingCount.text = "0"
                    } else {
                        binding.tvFollowingCount.text = (snap?.size() ?: 0).toString()
                    }
                }

            // Click listeners for followers/following
            binding.tvFollowersCount.setOnClickListener {
                UserListDialogFragment.newInstance("followers", currentUid)
                    .show(parentFragmentManager, "user_list")
            }
            binding.tvFollowingCount.setOnClickListener {
                UserListDialogFragment.newInstance("following", currentUid)
                    .show(parentFragmentManager, "user_list")
            }

            // Allow tapping avatar to change profile photo
            binding.profileAvatar.setOnClickListener {
                if (editMode) {
                    ensurePermissionAndPickAvatar()
                }
            }

            // Listen to user document to display name, bio and avatar updates in real time
            firestore.collection("users").document(currentUid)
                .addSnapshotListener(requireActivity()) { doc, e ->
                    if (_binding == null) return@addSnapshotListener
                    if (e != null || doc == null || !doc.exists()) {
                        val fallback = auth.currentUser?.displayName ?: auth.currentUser?.email ?: "username"
                        binding.profileDisplayName.text = fallback
                        binding.profileUsername.text = "@${fallback}"
                        binding.profileBio.text = "no bio"
                        binding.profileVerifiedBadge.visibility = View.GONE
                        return@addSnapshotListener
                    }

                    val displayName = doc.getString("visibleName") ?: doc.getString("displayName")
                    val userid = doc.getString("userid")
                        ?: doc.getString("username")
                        ?: auth.currentUser?.email
                        ?: "username"

                    binding.profileDisplayName.text = displayName ?: userid
                    binding.profileUsername.text = "@${userid}"
                    val typeFlag = doc.getString("userType")?.lowercase(Locale.US) ?: ""
                    val explicitVerified = doc.getBoolean("isVerified") ?: doc.getBoolean("verified") ?: false
                    val isVerified = explicitVerified || typeFlag == "verified"
                    binding.profileVerifiedBadge.visibility = if (isVerified) View.VISIBLE else View.GONE

                    // Bio (limit to 50 words)
                    val bio = doc.getString("bio") ?: ""
                    val words = bio.trim().split(Regex("\\s+"))
                    val bioLimited = if (words.size <= 50) bio.trim() else words.take(50).joinToString(" ")
                    binding.profileBio.text = if (bioLimited.isBlank()) "no bio" else bioLimited

                    val photoUrl = doc.getString("photoUrl") ?: auth.currentUser?.photoUrl?.toString()
                    val logoId = resources.getIdentifier("LogoProfile", "drawable", requireContext().packageName)
                    val fallbackId = if (logoId != 0) logoId else R.drawable.tempologo
                    if (!photoUrl.isNullOrBlank()) {
                        binding.profileAvatar.load(photoUrl) {
                            transformations(CircleCropTransformation())
                            placeholder(fallbackId)
                            error(fallbackId)
                        }
                    } else {
                        binding.profileAvatar.setImageResource(fallbackId)
                    }
                }

            // Tapping the display name opens an edit dialog (max 5 changes per day)
            binding.profileDisplayName.setOnClickListener {
                if (currentUid == null) {
                    Toast.makeText(requireContext(), "No hay usuario autenticado", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (!editMode) return@setOnClickListener

                if (!canEditVisibleName(currentUid)) {
                    Toast.makeText(requireContext(), "Has alcanzado el límite de 5 cambios hoy", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val edit = EditText(requireContext())
                edit.setText(binding.profileDisplayName.text)

                AlertDialog.Builder(requireContext())
                    .setTitle("Editar nombre visible")
                    .setView(edit)
                    .setPositiveButton("Guardar") { dialog, _ ->
                        val newName = edit.text.toString().trim()
                        if (newName.isEmpty()) {
                            Toast.makeText(requireContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }

                        // Update Firestore (merge)
                        firestore.collection("users").document(currentUid)
                            .set(mapOf("visibleName" to newName), com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener {
                                incrementEditCount(currentUid)
                                Toast.makeText(requireContext(), "Nombre visible actualizado", Toast.LENGTH_SHORT).show()
                                editsMade = true
                                updateEditButtonLabel()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Error actualizando nombre: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        } else {
            // Fallback for when there is no logged-in user
            binding.profileDisplayName.text = "Username"
            binding.profileUsername.text = "@username"
        }

        // Allow editing bio (max 50 words) by tapping the bio text
        binding.profileBio.setOnClickListener {
            val auth = FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid
            if (uid == null) {
                Toast.makeText(requireContext(), "No hay usuario autenticado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!editMode) return@setOnClickListener

            val edit = EditText(requireContext())
            edit.setText(binding.profileBio.text)

            AlertDialog.Builder(requireContext())
                .setTitle("Editar descripción del perfil (máx. 50 palabras)")
                .setView(edit)
                .setPositiveButton("Guardar") { _, _ ->
                    val raw = edit.text.toString().trim()
                    val words = if (raw.isEmpty()) emptyList() else raw.split(Regex("\\s+"))
                    val newBio = if (words.size <= 50) raw else words.take(50).joinToString(" ")
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .set(mapOf("bio" to newBio), com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Descripción actualizada", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Error actualizando descripción: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // --- End of Changes ---
    }

    // Called after avatar upload completes successfully
    private fun onAvatarUpdated() {
        editsMade = true
        updateEditButtonLabel()
    }

    private fun enterEditMode() {
        editMode = true
        editsMade = false
        // Keep pill style from XML; only update label
        binding.btnEditProfile.text = "Volver"

        // Show edit indicators
        showEditIndicators(true)

        // Enable tap-to-edit on avatar, name and bio
        binding.profileAvatar.isClickable = true
        binding.profileDisplayName.isClickable = true
        binding.profileBio.isClickable = true
    }

    private fun exitEditMode() {
        editMode = false
        editsMade = false
        // Restore button label; keep pill style from XML
        binding.btnEditProfile.text = "Editar perfil"

        // Hide indicators
        showEditIndicators(false)
        // Disable tap-to-edit
        binding.profileAvatar.isClickable = false
        binding.profileDisplayName.isClickable = false
        binding.profileBio.isClickable = false
    }

    private fun updateEditButtonLabel() {
        if (editMode) {
            // Show 'Volver' while in edit mode to allow exiting anytime
            binding.btnEditProfile.text = "Volver"
        } else {
            binding.btnEditProfile.text = "Editar perfil"
        }
    }

    private fun showEditIndicators(show: Boolean) {
        val v = if (show) View.VISIBLE else View.GONE
        binding.editIndicatorAvatar.visibility = v
        binding.editIndicatorName.visibility = v
        binding.editIndicatorBio.visibility = v
    }

    private fun startShake(target: View) {
        // Subtle shake animation (horizontal)
        val animator = android.animation.ObjectAnimator.ofFloat(target, View.TRANSLATION_X, -3f, 3f)
        animator.repeatMode = android.animation.ValueAnimator.REVERSE
        animator.repeatCount = android.animation.ValueAnimator.INFINITE
        animator.duration = 600
        animator.start()
    }

    private fun setTabsState(showPosts: Boolean) {
        // Toggle content visibility
        binding.rvFeed.visibility = if (showPosts) View.VISIBLE else View.GONE
        binding.tvItemsComing.visibility = if (showPosts) View.GONE else View.VISIBLE

        // Update tab visuals
        if (showPosts) {
            // Posts active
            binding.tabPosts.setBackgroundColor(android.graphics.Color.parseColor("#222222"))
            binding.tabPosts.setTextColor(android.graphics.Color.WHITE)
            binding.tabItems.setBackgroundColor(android.graphics.Color.parseColor("#DDDDDD"))
            binding.tabItems.setTextColor(android.graphics.Color.BLACK)
        } else {
            // Items active
            binding.tabItems.setBackgroundColor(android.graphics.Color.parseColor("#222222"))
            binding.tabItems.setTextColor(android.graphics.Color.WHITE)
            binding.tabPosts.setBackgroundColor(android.graphics.Color.parseColor("#DDDDDD"))
            binding.tabPosts.setTextColor(android.graphics.Color.BLACK)
        }
    }

    private fun onPostClicked(post: Post) {
        // Abre un fragmento de detalle con imagen completa y descripción
        val detail = com.onfu.app.ui.post.PostDetailFragment.newInstanceForUser(post.ownerId)
        parentFragmentManager
            .beginTransaction()
            .replace(R.id.home_child_container, detail)
            .addToBackStack(null)
            .commit()
    }

    private fun ensurePermissionAndPickAvatar() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            pickAvatarLauncher.launch("image/*")
        } else {
            permissionLauncher.launch(permission)
        }
    }

    private fun uploadAvatarUri(uri: Uri) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "No hay usuario autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(requireContext(), "Subiendo foto de perfil...", Toast.LENGTH_SHORT).show()

        val storageRef = FirebaseStorage.getInstance().reference.child("avatars/$uid/profile_${System.currentTimeMillis()}.jpg")
        val uploadTask = storageRef.putFile(uri)
        uploadTask.addOnSuccessListener { _ ->
            storageRef.downloadUrl
                .addOnSuccessListener { uriDownload ->
                    val downloadUrl = uriDownload.toString()
                    // update Firestore (use merge to be robust)
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .set(mapOf("photoUrl" to downloadUrl), com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener {
                            // update auth profile
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setPhotoUri(Uri.parse(downloadUrl))
                                .build()
                            auth.currentUser?.updateProfile(profileUpdates)

                            // update UI immediately (safely, view may have been destroyed)
                            _binding?.let { b ->
                                val logoId = resources.getIdentifier("logo_profile", "drawable", requireContext().packageName)
                                val placeholderId = if (logoId != 0) logoId else R.drawable.tempologo
                                b.profileAvatar.load(downloadUrl) {
                                    transformations(CircleCropTransformation())
                                    placeholder(placeholderId)
                                    error(placeholderId)
                                }
                            }

                            Toast.makeText(requireContext(), "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Error guardando photoUrl: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("FeedFragment", "Failed to fetch downloadUrl", e)
                    Toast.makeText(requireContext(), "Failed upload: unable to get download URL: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }.addOnFailureListener { e ->
            android.util.Log.e("FeedFragment", "Upload failed", e)
            Toast.makeText(requireContext(), "Fallo al subir la imagen: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Helpers to limit visible name edits to 5 times per day per user
    private fun getTodayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    private fun canEditVisibleName(uid: String): Boolean {
        val prefs = requireContext().getSharedPreferences("onfu_prefs", Context.MODE_PRIVATE)
        val storedDate = prefs.getString("${uid}_date", "") ?: ""
        val storedCount = prefs.getInt("${uid}_count", 0)
        val today = getTodayString()
        return if (storedDate == today) {
            storedCount < 5
        } else {
            true
        }
    }

    private fun incrementEditCount(uid: String) {
        val prefs = requireContext().getSharedPreferences("onfu_prefs", Context.MODE_PRIVATE)
        val today = getTodayString()
        val keyDate = "${uid}_date"
        val keyCount = "${uid}_count"
        val storedDate = prefs.getString(keyDate, "") ?: ""
        val newCount = if (storedDate == today) prefs.getInt(keyCount, 0) + 1 else 1
        prefs.edit().putString(keyDate, today).putInt(keyCount, newCount).apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Removed helper limitToMaxWords; logic is inlined where needed

    // Caption edit disabled for grid feed (bio editable in header)
}