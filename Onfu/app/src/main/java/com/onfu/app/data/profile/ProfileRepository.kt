package com.onfu.app.data.profile

import com.google.firebase.firestore.FirebaseFirestore
import com.onfu.app.domain.models.User
import kotlinx.coroutines.tasks.await

class ProfileRepository(private val firestore: FirebaseFirestore) {

    private val usersCollection = firestore.collection("users")

    suspend fun createOrUpdateProfile(user: User) {
        usersCollection.document(user.uid).set(user).await()
    }

    suspend fun getUser(uid: String): User? {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
