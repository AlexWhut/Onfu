package com.onfu.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.onfu.app.R
import com.onfu.app.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val email = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // If input looks like an email (contains @) use email/password sign-in.
            if (email.contains("@")) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                            checkProfileExistsAndNavigate(uid)
                        } else {
                            Toast.makeText(requireContext(), "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                // Treat input as username: resolve to email via Firestore users collection
                val candidate = email
                firestore.collection("users").whereEqualTo("userid", candidate).get()
                    .addOnSuccessListener { snaps ->
                        if (snaps.isEmpty) {
                            Toast.makeText(requireContext(), "Usuario no encontrado. Usa tu correo para iniciar sesión.", Toast.LENGTH_LONG).show()
                            return@addOnSuccessListener
                        }
                        val doc = snaps.documents[0]
                        val resolvedEmail = doc.getString("email")
                        if (resolvedEmail.isNullOrEmpty()) {
                            Toast.makeText(requireContext(), "Este usuario no tiene email registrado. Usa el correo para iniciar sesión.", Toast.LENGTH_LONG).show()
                            return@addOnSuccessListener
                        }
                        // sign in with resolved email
                        auth.signInWithEmailAndPassword(resolvedEmail, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                                    checkProfileExistsAndNavigate(uid)
                                } else {
                                    Toast.makeText(requireContext(), "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    }
                    .addOnFailureListener { err ->
                        Toast.makeText(requireContext(), "Error buscando usuario: ${err.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }

        binding.btnGoogle.setOnClickListener {
            // TODO: start Google sign-in flow
            Toast.makeText(requireContext(), "Login with Google (not implemented)", Toast.LENGTH_SHORT).show()
        }

        binding.tvRegisterLink.setOnClickListener {
            // Navigate to RegisterFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun checkProfileExistsAndNavigate(uid: String) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // already has profile -> go to Home
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, PlaceholderFragment.newInstance("Home"))
                        .commit()
                } else {
                    // go to prehome to set userid/name
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, PreHomeFragment())
                        .commit()
                }
            }
            .addOnFailureListener { err ->
                Toast.makeText(requireContext(), "Error: ${err.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
