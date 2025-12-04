package com.onfu.app.ui.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.onfu.app.R
import com.onfu.app.databinding.FragmentChatListBinding
import com.onfu.app.data.messages.MessagesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.android.material.snackbar.Snackbar

class ChatListFragment : Fragment() {
    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!
    private val repo = MessagesRepository()
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private val adapter by lazy { ChatListAdapter { conversationId ->
        parentFragmentManager.beginTransaction()
            .replace(R.id.home_child_container, ChatFragment.newInstance(conversationId))
            .addToBackStack(null)
            .commit()
    } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        val uid = auth.currentUser?.uid ?: return
        repo.conversationsQuery(uid)
            .addSnapshotListener(requireActivity()) { snap, err ->
                if (_binding == null) return@addSnapshotListener
                if (err != null || snap == null) {
                    // No vaciamos la lista en caso de error; mostramos mensaje específico y conservamos datos visibles
                    val ffErr = err as? FirebaseFirestoreException
                    if (ffErr?.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                        // Índice faltante o en construcción
                        Snackbar.make(binding.root, "Consulta requiere índice. Crea el índice en Firebase Console.", Snackbar.LENGTH_LONG).show()
                    } else {
                        android.widget.Toast.makeText(requireContext(), "Error cargando conversaciones.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    val shortMsg = ffErr?.code?.name ?: err?.javaClass?.simpleName ?: "UnknownError"
                    android.util.Log.w("ChatListFragment", "conversations listener error: $shortMsg")
                } else {
                    val items = snap.documents.map { d ->
                        ChatListItem(
                            id = d.id,
                            otherUid = (d.get("participants") as? List<*>)?.firstOrNull { it != uid } as? String ?: "",
                            lastText = d.getString("lastMessageText") ?: "",
                            lastAt = d.getLong("lastMessageAt") ?: 0L
                        )
                    }
                    adapter.submitList(items)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class ChatListItem(
    val id: String,
    val otherUid: String,
    val lastText: String,
    val lastAt: Long
)

