package com.onfu.app.domain.usecases

import com.onfu.app.domain.models.Post
import com.onfu.app.data.post.PostRepository

class PostUseCase(private val repository: PostRepository) {

    suspend fun uploadPost(post: Post, imageBytes: ByteArray) {
        repository.uploadPost(post, imageBytes)
    }

    suspend fun getPosts() = repository.getAllPosts()
}