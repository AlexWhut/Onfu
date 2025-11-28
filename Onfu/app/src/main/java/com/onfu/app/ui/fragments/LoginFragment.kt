package com.onfu.app.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.onfu.app.R
import com.onfu.app.databinding.FragmentLoginBinding
import com.onfu.app.ui.fragments.HomeFragment

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var googleSignInClient: GoogleSignInClient? = null

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

        // Configurar Google Sign-In
        // `default_web_client_id` debe existir en resources (generado por google-services)
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(com.onfu.app.R.string.default_web_client_id))
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        } catch (e: Exception) {
            // Si falta default_web_client_id en strings.xml, informar
            e.printStackTrace()
        }

        val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data: Intent? = result.data
            Log.d("LoginFragment", "GoogleSignIn resultCode=${result.resultCode}, data=${data != null}")
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    val account = task.getResult(ApiException::class.java)
                    val idToken = account?.idToken
                    if (!idToken.isNullOrEmpty()) {
                        // Intercambiar idToken por credenciales Firebase
                        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                            .addOnCompleteListener { taskAuth ->
                                if (taskAuth.isSuccessful) {
                                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                                    checkProfileExistsAndNavigate(uid)
                                } else {
                                    Log.w("LoginFragment", "Firebase signInWithCredential failed", taskAuth.exception)
                                    Toast.makeText(requireContext(), "Google sign-in failed: ${taskAuth.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        Log.w("LoginFragment", "No ID token returned by Google account")
                        Toast.makeText(requireContext(), "No ID token from Google Sign-In", Toast.LENGTH_LONG).show()
                    }
                } catch (apiEx: ApiException) {
                    Log.w("LoginFragment", "Google sign-in ApiException", apiEx)
                    Toast.makeText(requireContext(), "Google sign-in error: ${apiEx.statusCode} - ${apiEx.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                // Si se cancela, intentar extraer más información si está disponible
                if (data != null) {
                    try {
                        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                        val account = task.getResult(ApiException::class.java)
                        Log.d("LoginFragment", "Cancelled but account present: ${account?.email}")
                    } catch (apiEx: ApiException) {
                        Log.w("LoginFragment", "GoogleSignIn cancelled, ApiException: ${apiEx.statusCode}", apiEx)
                        Toast.makeText(requireContext(), "Google sign-in canceled (code ${apiEx.statusCode})", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Google sign-in canceled", Toast.LENGTH_SHORT).show()
                }
            }
        }

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
            val client = googleSignInClient
            if (client == null) {
                Toast.makeText(requireContext(), "Google Sign-In no está configurado. Revisa `default_web_client_id`.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val signInIntent = client.signInIntent
            googleSignInLauncher.launch(signInIntent)
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
                    // already has profile -> go to HomeFragment (host with bottom nav)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HomeFragment())
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
