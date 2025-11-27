package com.onfu.app.domain.usecases

import com.onfu.app.data.auth.AuthRepository
import com.google.firebase.auth.FirebaseUser

class AuthUseCase(private val repository: AuthRepository) {

    val currentUser: FirebaseUser?
        get() = repository.currentUser

    suspend fun register(email: String, password: String) = repository.register(email, password)
    suspend fun login(email: String, password: String) = repository.login(email, password)
    fun logout() = repository.logout()
}