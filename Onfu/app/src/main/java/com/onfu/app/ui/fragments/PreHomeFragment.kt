package com.onfu.app.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.onfu.app.R
import com.onfu.app.databinding.FragmentPrehomeBinding

class PreHomeFragment : Fragment() {

    private var _binding: FragmentPrehomeBinding? = null
    private val binding get() = _binding!!
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPrehomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etUserId.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val candidate = s?.toString()?.trim() ?: ""
                checkAvailability(candidate)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnContinue.setOnClickListener {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                Toast.makeText(requireContext(), "Not authenticated", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userid = binding.etUserId.text.toString().trim()
            if (userid.isEmpty()) {
                binding.tvAvailability.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                binding.tvAvailability.text = "Username is required"
                return@setOnClickListener
            }
            if (userid.length > 10) {
                binding.tvAvailability.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                binding.tvAvailability.text = "Max 10 characters"
                return@setOnClickListener
            }

            // Check availability one more time before saving
            firestore.collection("users").whereEqualTo("userid", userid).get()
                .addOnSuccessListener { snaps ->
                    val conflict = snaps.documents.any { it.getString("uid") != uid }
                    if (conflict) {
                        binding.tvAvailability.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                        binding.tvAvailability.text = "No disponible"
                        return@addOnSuccessListener
                    }

                    var visibleName = binding.etVisibleName.text.toString().trim()
                    if (visibleName.isEmpty()) visibleName = userid

                    val email = auth.currentUser?.email
                    val data = mapOf(
                        "uid" to uid,
                        "userid" to userid,
                        "visibleName" to visibleName,
                        "photoUrl" to null,
                        "email" to email
                    )

                    firestore.collection("users").document(uid).set(data)
                        .addOnSuccessListener {
                            // Navigate to HomeFragment (host with bottom nav)
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, HomeFragment())
                                .commit()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Error saving profile: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error checking username: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun checkAvailability(candidate: String) {
        if (candidate.isEmpty()) {
            binding.tvAvailability.text = ""
            return
        }

        firestore.collection("users").whereEqualTo("userid", candidate).get()
            .addOnSuccessListener { snaps ->
                val uid = auth.currentUser?.uid
                val existsOther = snaps.documents.any { it.getString("uid") != uid }
                if (existsOther) {
                    binding.tvAvailability.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                    binding.tvAvailability.text = "No disponible"
                } else {
                    binding.tvAvailability.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
                    binding.tvAvailability.text = "Disponible"
                }
            }
            .addOnFailureListener { e ->
                binding.tvAvailability.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                binding.tvAvailability.text = "Error"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
