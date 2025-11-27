package com.onfu.app.presentation.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onfu.app.domain.usecases.AuthUseCase
import com.onfu.app.domain.usecases.PostUseCase
import com.onfu.app.domain.models.Post
import kotlinx.coroutines.launch

class PostViewModel(
    private val authUseCase: AuthUseCase,
    private val postUseCase: PostUseCase
) : ViewModel() {

    val _errorMessage = MutableLiveData<String>()

    fun uploadPost(title: String, description: String, imageBytes: ByteArray, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val userId = authUseCase.currentUser?.uid ?: ""

                if (userId.isEmpty()) {
                    _errorMessage.value = "No user is currently logged in."
                    return@launch
                }

                val post = Post(
                    ownerId = userId,
                    title = title,
                    description = description
                )

                postUseCase.uploadPost(post, imageBytes)

                onSuccess()
                loadPosts()

            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }

    }

    private fun loadPosts() {
        // Aquí cargas los posts a tu LiveData o StateFlow.
    }
}
