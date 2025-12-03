package com.onfu.app.data.messages

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.onfu.app.domain.models.Conversation
import com.onfu.app.domain.models.Message

class MessagesRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun conversationsQuery(uid: String) = firestore.collection("conversations")
        .whereArrayContains("participants", uid)
        .orderBy("lastMessageAt", Query.Direction.DESCENDING)

    fun messagesQuery(conversationId: String) = firestore.collection("conversations")
        .document(conversationId)
        .collection("messages")
        .orderBy("createdAt", Query.Direction.ASCENDING)

    fun sendMessage(conversationId: String, text: String, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onComplete(false)
        val msgRef = firestore.collection("conversations").document(conversationId)
            .collection("messages").document()
        val now = System.currentTimeMillis()
        val data = mapOf(
            "id" to msgRef.id,
            "conversationId" to conversationId,
            "senderId" to uid,
            "text" to text,
            "createdAt" to now
        )
        firestore.runBatch { b ->
            b.set(msgRef, data)
            val convRef = firestore.collection("conversations").document(conversationId)
            b.set(
                convRef,
                mapOf(
                    "lastMessageText" to text,
                    "lastMessageSenderId" to uid,
                    "lastMessageAt" to now
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
        }.addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun startConversation(otherUid: String, onComplete: (String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onComplete(null)
        val existingQuery = firestore.collection("conversations")
            .whereArrayContains("participants", uid)
        existingQuery.get().addOnSuccessListener { snap ->
            val found = snap.documents.firstOrNull { d ->
                val parts = d.get("participants") as? List<*> ?: emptyList<Any>()
                parts.contains(otherUid)
            }
            if (found != null) {
                onComplete(found.id)
            } else {
                val convRef = firestore.collection("conversations").document()
                val conv = mapOf(
                    "id" to convRef.id,
                    "participants" to listOf(uid, otherUid),
                    "lastMessageText" to "",
                    "lastMessageSenderId" to "",
                    "lastMessageAt" to System.currentTimeMillis()
                )
                convRef.set(conv).addOnSuccessListener { onComplete(convRef.id) }
                    .addOnFailureListener { onComplete(null) }
            }
        }.addOnFailureListener { onComplete(null) }
    }
}
