package com.onfu.app.ui.search

data class SearchUser(
    val uid: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val bio: String,
    val isVerified: Boolean
)

data class SearchResultUi(
    val user: SearchUser,
    val isFollowing: Boolean,
    val isLoading: Boolean
)
