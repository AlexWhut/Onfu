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
        val imageRef = storage.reference.child("posts/${UUID.randomUUID()}.jpg")
        val uploadTask = imageRef.putBytes(imageBytes).await()
        val downloadUrl = imageRef.downloadUrl.await().toString()

        val postWithImage = post.copy(imageUrl = downloadUrl)
        postsCollection.add(postWithImage).await()
    }

    suspend fun getAllPosts(): List<Post> {
        val snapshot = postsCollection.get().await()
        return snapshot.toObjects(Post::class.java)
    }
}
