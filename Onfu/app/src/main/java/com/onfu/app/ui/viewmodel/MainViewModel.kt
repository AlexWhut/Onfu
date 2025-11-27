package com.onfu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onfu.app.domain.models.Post
import com.onfu.app.domain.models.User
import com.onfu.app.domain.usecases.AuthUseCase
import com.onfu.app.domain.usecases.PostUseCase
import com.onfu.app.domain.usecases.ProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val authUseCase: AuthUseCase,
    private val profileUseCase: ProfileUseCase,
    private val postUseCase: PostUseCase
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> get() = _currentUser

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> get() = _posts

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage

    // Login
    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val user = authUseCase.login(email, password)
                user?.let {
                    val profile = profileUseCase.getUser(it.uid)
                    _currentUser.value = profile
                    onSuccess()
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    // Register
    fun register(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val user = authUseCase.register(email, password)
                user?.let {
                    val newUser = User(uid = it.uid, email = email)
                    profileUseCase.createOrUpdateProfile(newUser)
                    _currentUser.value = newUser
                    onSuccess()
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    // Logout
    fun logout() {
        authUseCase.logout()
        _currentUser.value = null
    }

    // Upload Post
    fun uploadPost(title: String, description: String, imageBytes: ByteArray, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val userId = authUseCase.currentUser?.uid ?: return@launch
                val post = Post(ownerId = userId, title = title, description = description)
                postUseCase.uploadPost(post, imageBytes)
                onSuccess()
                loadPosts() // refresh feed
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    // Load posts for feed
    fun loadPosts() {
        viewModelScope.launch {
            try {
                val allPosts = postUseCase.getPosts()
                _posts.value = allPosts
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }
}
