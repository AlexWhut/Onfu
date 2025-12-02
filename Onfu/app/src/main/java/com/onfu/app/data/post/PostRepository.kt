package com.onfu.app.data.post

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.onfu.app.domain.models.Post
import kotlinx.coroutines.tasks.await
import java.util.*

class PostRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val postsCollection = firestore.collection("posts")

    suspend fun uploadPost(post: Post, imageBytes: ByteArray) {
        // Upload image bytes to Firebase Storage under posts/{ownerId}/...
        val ownerId = post.ownerId
        val filename = "${System.currentTimeMillis()}.jpg"
        val imageRef = storage.reference.child("posts/$ownerId/$filename")
        imageRef.putBytes(imageBytes).await()
        val downloadUrl = imageRef.downloadUrl.await().toString()

        // Save post to Firestore and persist generated document id
        val postWithImage = post.copy(imageUrl = downloadUrl, timestamp = System.currentTimeMillis())
        val docRef = postsCollection.add(postWithImage).await()
        // Update the document's id field for easier client-side handling
        postsCollection.document(docRef.id).update(mapOf("id" to docRef.id)).await()
    }

    suspend fun getAllPosts(): List<Post> {
        val snapshot = postsCollection.get().await()
        return snapshot.toObjects(Post::class.java)
    }
}
