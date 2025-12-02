package com.onfu.app.ui.upload

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.onfu.app.R
import com.onfu.app.data.post.PostRepository
import com.onfu.app.domain.models.Post
import kotlinx.coroutines.launch

class UploadFragment : Fragment() {

    private lateinit var imagePreview: ImageView
    private lateinit var descriptionInput: EditText
    private lateinit var selectButton: Button
    private lateinit var submitButton: Button

    private var selectedUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedUri = uri
        uri?.let { imagePreview.setImageURI(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_upload, container, false)
        imagePreview = view.findViewById(R.id.iv_upload_preview)
        descriptionInput = view.findViewById(R.id.et_upload_description)
        selectButton = view.findViewById(R.id.btn_select_image)
        submitButton = view.findViewById(R.id.btn_submit_post)

        selectButton.setOnClickListener { pickImage.launch("image/*") }
        submitButton.setOnClickListener { submitPost() }

        return view
    }

    private fun submitPost() {
        val uri = selectedUri ?: return
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser ?: return

        val bytes = readBytesFromUriResized(uri, targetWidth = 1360, maxHeight = 1080) ?: return

        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()
        val repo = PostRepository(firestore, storage)

        val post = Post(
            ownerId = currentUser.uid,
            title = "",
            description = descriptionInput.text?.toString() ?: "",
            imageUrl = "",
            timestamp = System.currentTimeMillis(),
            id = ""
        )

        lifecycleScope.launch {
            try {
                repo.uploadPost(post, bytes)
                // Optionally clear UI after success
                selectedUri = null
                imagePreview.setImageDrawable(null)
                descriptionInput.setText("")
            } catch (e: Exception) {
                // You can show a Toast or log the error
            }
        }
    }

    private fun readBytesFromUriResized(uri: Uri, targetWidth: Int, maxHeight: Int): ByteArray? {
        return try {
            val original = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            }

            val aspect = original.width.toFloat() / original.height.toFloat()
            val targetH = (targetWidth / aspect).toInt()
            val finalH = if (targetH > maxHeight) maxHeight else targetH
            val finalW = if (targetH > maxHeight) (maxHeight * aspect).toInt() else targetWidth

            val resized = android.graphics.Bitmap.createScaledBitmap(original, finalW, finalH, true)
            val stream = java.io.ByteArrayOutputStream()
            resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}
