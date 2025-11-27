package com.onfu.app.domain.usecases

import com.onfu.app.data.profile.ProfileRepository
import com.onfu.app.domain.models.User

class ProfileUseCase(private val repository: ProfileRepository) {

    suspend fun createOrUpdateProfile(user: User) = repository.createOrUpdateProfile(user)
    suspend fun getUser(uid: String) = repository.getUser(uid)
}
