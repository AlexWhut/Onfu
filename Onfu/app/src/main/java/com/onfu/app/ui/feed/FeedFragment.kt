package com.onfu.app.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        // Setup RecyclerView: vertical list of posts
        val rv = binding.rvPosts
        rv.layoutManager = LinearLayoutManager(requireContext())

        // Sample data - replace with real repository / ViewModel
        val sample = listOf(
            Post(ownerId = "u1", title = "At the beach", description = "Nice day", imageUrl = "", id = "p1"),
            Post(ownerId = "u2", title = "Mountain", description = "Hike", imageUrl = "", id = "p2"),
            Post(ownerId = "u3", title = "City", description = "Night lights", imageUrl = "", id = "p3")
        )

        rv.adapter = FeedAdapter(sample) { post ->
            onPostClicked(post)
        }

        // Load and display the current user's username in the profile header
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val uid = auth.currentUser?.uid
        if (uid != null) {
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val username = doc.getString("userid")
                        ?: doc.getString("username")
                        ?: doc.getString("displayName")
                        ?: auth.currentUser?.displayName
                        ?: auth.currentUser?.email
                        ?: "Username"
                    binding.profileUsername.text = username
                }
                .addOnFailureListener {
                    binding.profileUsername.text = auth.currentUser?.displayName ?: auth.currentUser?.email ?: "Username"
                }
        } else {
            binding.profileUsername.text = auth.currentUser?.displayName ?: auth.currentUser?.email ?: "Username"
        }
    }

    private fun onPostClicked(post: Post) {
        // TODO: abrir detalle del post (Fragment o Activity) mostrando imagen, likes y descripción.
        // Ejemplo con Navigation Component:
        // val action = FeedFragmentDirections.actionFeedToPostDetail(post.id)
        // findNavController().navigate(action)

        Toast.makeText(requireContext(), "Open post ${post.title}", Toast.LENGTH_SHORT).show()
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
            val image: ImageView = itemView.findViewById(R.id.post_image)
            val caption: TextView = itemView.findViewById(R.id.post_caption)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val post = items[position]
            holder.caption.text = "@${post.ownerId}  •  ${post.title}"

            // Placeholder: carga de imagen aquí.
            // Si usas Glide: Glide.with(holder.image).load(post.imageUrl).into(holder.image)

            holder.itemView.setOnClickListener { onClick(post) }
        }

        override fun getItemCount(): Int = items.size
    }
}
