package com.onfu.app.ui.fragments

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.fragment.app.Fragment
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.FirebaseApp
import android.content.pm.ApplicationInfo
import com.google.firebase.storage.ListResult
import com.onfu.app.R

class UploadFragment : Fragment() {

    companion object {
    }

    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            view?.findViewById<ImageView>(R.id.iv_upload_preview)?.load(it) {
                placeholder(android.R.drawable.sym_def_app_icon)
                error(android.R.drawable.sym_def_app_icon)
            }
        }
    }

    // Return a FirebaseStorage instance configured for this project/bucket.
    private fun getConfiguredStorageInstance(): FirebaseStorage {
        return try {
            val bucket = FirebaseApp.getInstance().options.storageBucket
            Log.d("STORAGE_DEBUG", "FirebaseApp.options.storageBucket = ${bucket}")
            if (!bucket.isNullOrBlank()) {
                val bucketUrl = if (bucket.startsWith("gs://")) bucket else "gs://$bucket"
                Log.d("STORAGE_DEBUG", "Using configured bucket: $bucketUrl")
                FirebaseStorage.getInstance(bucketUrl)
            } else {
                Log.d("STORAGE_DEBUG", "No storage bucket in FirebaseApp options — using default FirebaseStorage instance")
                FirebaseStorage.getInstance()
            }
        } catch (ex: Exception) {
            Log.w("STORAGE_DEBUG", "Could not determine storage bucket from FirebaseApp options: ${ex.message}")
            FirebaseStorage.getInstance()
        }
    }

    // Permission launcher for reading images (handles both legacy and Android 13+)
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            // Permission granted -> launch picker
            pickImageLauncher.launch("image/*")
        } else {
            Toast.makeText(requireContext(), "Permiso denegado: no se puede acceder a las fotos", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_upload, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Emulator usage removed: all Storage operations will go against the configured HTTPS bucket

            view.findViewById<Button>(R.id.btn_select_image).setOnClickListener {
                // Check runtime permission if needed, then launch picker
                ensurePermissionAndPick()
            }

            view.findViewById<Button>(R.id.btn_submit_post).setOnClickListener {
                // If an image is selected, upload as avatar (for profile picture). Otherwise show a message.
                if (selectedImageUri == null) {
                    Toast.makeText(requireContext(), "Selecciona una imagen primero", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                uploadSelectedImage(selectedImageUri!!)
            }
    }

    private fun uploadSelectedImage(uri: Uri) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "No hay usuario autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(requireContext(), "Subiendo imagen...", Toast.LENGTH_SHORT).show()

        val storage = getConfiguredStorageInstance()
        val storageRef = storage.reference.child("avatars/$uid/${System.currentTimeMillis()}.jpg")
        val uploadTask = storageRef.putFile(uri)
        uploadTask.addOnSuccessListener { taskSnapshot ->
            // Upload succeeded, now get download URL
            storageRef.downloadUrl
                .addOnSuccessListener { uriDownload ->
                    val downloadUrl = uriDownload.toString()
                    // Update Firestore users/{uid}.photoUrl (use merge to ensure document exists)
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .set(mapOf("photoUrl" to downloadUrl), SetOptions.merge())
                        .addOnSuccessListener {
                            // Update FirebaseAuth profile photo (optional)
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setPhotoUri(Uri.parse(downloadUrl))
                                .build()
                            auth.currentUser?.updateProfile(profileUpdates)

                            // Update preview immediately
                            view?.findViewById<ImageView>(R.id.iv_upload_preview)?.load(downloadUrl) {
                                placeholder(android.R.drawable.sym_def_app_icon)
                                error(android.R.drawable.sym_def_app_icon)
                                transformations(coil.transform.CircleCropTransformation())
                            }

                            Toast.makeText(requireContext(), "Imagen subida y perfil actualizado", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e("UploadFragment", "Failed to write photoUrl for $uid", e)
                            Toast.makeText(requireContext(), "Error guardando photoUrl: ${e.message}", Toast.LENGTH_LONG).show()
                            // Diagnostic: list objects after Firestore write failure
                            debugStorageState(uid)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("UploadFragment", "Failed to fetch downloadUrl", e)
                    Toast.makeText(requireContext(), "Failed upload: unable to get download URL: ${e.message}", Toast.LENGTH_LONG).show()
                    // Diagnostic: list objects if unable to get download URL
                    debugStorageState(uid)
                }
        }.addOnFailureListener { e ->
            Log.e("UploadFragment", "Upload failed", e)
            Toast.makeText(requireContext(), "Fallo al subir la imagen: ${e.message}", Toast.LENGTH_LONG).show()
            // Diagnostic: list objects after upload failure
            debugStorageState(uid)
        }
    }

    // Fallback check for debug mode without depending on BuildConfig (helps when generated BuildConfig is not available)
    private fun isAppDebuggable(): Boolean {
        return try {
            val ai: ApplicationInfo = requireContext().applicationInfo
            (ai.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (ex: Exception) {
            false
        }
    }

    // Diagnostic helper: log configured bucket and list objects under avatars/{uid}
    private fun debugStorageState(uid: String) {
        try {
            val storage = getConfiguredStorageInstance()
            val configuredBucket = try { FirebaseApp.getInstance().options.storageBucket } catch (e: Exception) { null }
            Log.d("STORAGE_DEBUG", "Configured storage bucket: $configuredBucket")

            val avatarsRef = storage.reference.child("avatars/$uid")
            avatarsRef.listAll()
                .addOnSuccessListener { listResult: ListResult ->
                    Log.d("STORAGE_DEBUG", "listAll success: prefixes=${listResult.prefixes.size}, items=${listResult.items.size}")
                    listResult.items.forEach { item ->
                        Log.d("STORAGE_DEBUG", "Item: ${item.path} | name=${item.name}")
                    }
                    listResult.prefixes.forEach { prefix ->
                        Log.d("STORAGE_DEBUG", "Prefix: ${prefix.path}")
                    }
                }
                .addOnFailureListener { exc ->
                    Log.e("STORAGE_DEBUG", "listAll failed: ${exc.message}", exc)
                }
        } catch (ex: Exception) {
            Log.e("STORAGE_DEBUG", "debugStorageState failed: ${ex.message}", ex)
        }
    }

    private fun ensurePermissionAndPick() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val ctx = requireContext()
        if (ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED) {
            pickImageLauncher.launch("image/*")
        } else {
            // Request permission
            permissionLauncher.launch(permission)
        }
    }
}
