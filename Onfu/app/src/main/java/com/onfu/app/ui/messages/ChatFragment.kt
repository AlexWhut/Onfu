package com.onfu.app.ui.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.onfu.app.databinding.FragmentChatBinding
import com.onfu.app.data.messages.MessagesRepository
import com.onfu.app.domain.models.Message
import com.google.firebase.firestore.FirebaseFirestore
import coil.load
import coil.transform.CircleCropTransformation

class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val repo = MessagesRepository()

    private val adapter by lazy { MessagesAdapter() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val conversationId = requireArguments().getString(ARG_CONV_ID) ?: run {
            Toast.makeText(requireContext(), "Conversación inválida", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }
        binding.messages.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        binding.messages.adapter = adapter

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        // Header info (other user's avatar and name)
        FirebaseFirestore.getInstance().collection("conversations").document(conversationId).get()
            .addOnSuccessListener { d ->
                val participants = (d.get("participants") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val me = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                val other = participants.firstOrNull { it != me }
                if (other != null) {
                    FirebaseFirestore.getInstance().collection("users").document(other).get()
                        .addOnSuccessListener { u ->
                            binding.headerName.text = u.getString("visibleName") ?: u.getString("userid") ?: "user"
                            val photo = u.getString("photoUrl")
                            if (!photo.isNullOrBlank()) {
                                binding.headerAvatar.load(photo) { transformations(CircleCropTransformation()) }
                            }
                        }
                }
            }

        // Messages stream
        repo.messagesQuery(conversationId).addSnapshotListener(requireActivity()) { snap, err ->
            if (_binding == null) return@addSnapshotListener
            if (err != null || snap == null) {
                adapter.submitList(emptyList())
            } else {
                val list = snap.toObjects(Message::class.java)
                adapter.submitList(list)
                binding.messages.scrollToPosition(list.lastIndex)
            }
        }

        binding.btnSend.setOnClickListener {
            val text = binding.input.text?.toString()?.trim() ?: ""
            if (text.isEmpty()) return@setOnClickListener
            repo.sendMessage(conversationId, text) {
                if (it) binding.input.text?.clear() else Toast.makeText(requireContext(), "Error enviando", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CONV_ID = "convId"
        fun newInstance(conversationId: String) = ChatFragment().apply {
            arguments = Bundle().apply { putString(ARG_CONV_ID, conversationId) }
        }
    }
}
