package com.onfu.app

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

fun testFirebaseConnection() {
    val db = Firebase.firestore

    db.collection("test")
        .add(hashMapOf("msg" to "Hola Firebase"))
        .addOnSuccessListener {
            Log.d("FIREBASE", "Conectado correctamente ✔")
        }
        .addOnFailureListener {
            Log.e("FIREBASE", "Error ❌", it)
        }
}
