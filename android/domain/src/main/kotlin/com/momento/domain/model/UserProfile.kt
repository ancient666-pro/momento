package com.momento.domain.model

data class UserProfile(
    val id: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String,
    val verified: Boolean = false,
    val followerCount: Long = 0,
    val followingCount: Long = 0,
    val bio: String = "",
    val category: String? = null,
    val location: String? = null,
    val online: Boolean = false
)
