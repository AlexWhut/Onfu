package com.onfu.app.ui.feed

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.Manifest
import android.content.pm.PackageManager
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.onfu.app.ui.feed.GridSpacingItemDecoration
import com.onfu.app.R
import com.onfu.app.databinding.FragmentFeedBinding
import com.onfu.app.domain.models.Post

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

    // Avatar change helpers
    private var selectedAvatarUri: Uri? = null
    private val pickAvatarLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedAvatarUri = it
            // show preview immediately
            binding.profileAvatar.load(it) {
                transformations(CircleCropTransformation())
            }
            // start upload
            uploadAvatarUri(it)
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pickAvatarLauncher.launch("image/*")
        } else {
            Toast.makeText(requireContext(), "Permiso denegado: no se puede acceder a las fotos", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
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

        // Load posts from Firestore and render images only in the grid
        firestore.collection("posts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener(requireActivity()) { snapshot, error ->
                if (_binding == null) return@addSnapshotListener
                if (error != null || snapshot == null) {
                    binding.rvFeed.adapter = FeedAdapter(emptyList()) { _ -> }
                    return@addSnapshotListener
                }
                val posts = snapshot.toObjects(Post::class.java)
                binding.rvFeed.adapter = FeedAdapter(posts) { post -> onPostClicked(post) }
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
                    ensurePermissionAndPickAvatar()
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
                        return@addSnapshotListener
                    }

                    val displayName = doc.getString("visibleName") ?: doc.getString("displayName")
                    val userid = doc.getString("userid")
                        ?: doc.getString("username")
                        ?: auth.currentUser?.email
                        ?: "username"

                    binding.profileDisplayName.text = displayName ?: userid
                    binding.profileUsername.text = "@${userid}"

                    // Bio (limit to 50 words)
                    val bio = doc.getString("bio") ?: ""
                    val words = bio.trim().split(Regex("\\s+"))
                    val bioLimited = if (words.size <= 50) bio.trim() else words.take(50).joinToString(" ")
                    binding.profileBio.text = if (bioLimited.isBlank()) "no bio" else bioLimited

                    val photoUrl = doc.getString("photoUrl") ?: auth.currentUser?.photoUrl?.toString()
                    if (!photoUrl.isNullOrBlank()) {
                        binding.profileAvatar.load(photoUrl) {
                            transformations(CircleCropTransformation())
                        }
                    }
                }

            // Tapping the display name opens an edit dialog (max 5 changes per day)
            binding.profileDisplayName.setOnClickListener {
                if (currentUid == null) {
                    Toast.makeText(requireContext(), "No hay usuario autenticado", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

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

    private fun onPostClicked(post: Post) {
        // Abre un fragmento de detalle con imagen completa y descripción
        val detail = com.onfu.app.ui.post.PostDetailFragment.newInstance(
            post.imageUrl,
            post.description
        )
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

                            // update UI immediately
                            binding.profileAvatar.load(downloadUrl) {
                                transformations(CircleCropTransformation())
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

    private class FeedAdapter(
        private val items: List<Post>,
        private val onClick: (Post) -> Unit
    ) : RecyclerView.Adapter<FeedAdapter.VH>() {

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val image: ImageView = itemView.findViewById(R.id.iv_post_image)
            val caption: TextView = itemView.findViewById(R.id.post_caption)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val post = items[position]
            // Grid is images-only; hide caption
            holder.caption.visibility = View.GONE
            holder.caption.text = ""
            holder.image.load(post.imageUrl)

            holder.itemView.setOnClickListener { onClick(post) }

            // No caption edit in grid
        }

        override fun getItemCount(): Int = items.size
    }

    // Removed helper limitToMaxWords; logic is inlined where needed

    // Caption edit disabled for grid feed (bio editable in header)
}
