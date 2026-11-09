package com.momento.domain.model

data class NearbyProfile(
    val profile: UserProfile,
    val age: Int? = null,
    val distanceKm: Double = 0.0,
    val matchPercent: Int = 0,
    val interests: List<String> = emptyList()
)

data class SparkCard(
    val id: String,
    val title: String,
    val author: UserProfile,
    val mediaUrl: String,
    val viewCount: Long = 0,
    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val durationSeconds: Int? = null
)

data class AudioRoom(
    val id: String,
    val title: String,
    val listenerCount: Long = 0,
    val hosts: List<UserProfile> = emptyList(),
    val artworkUrl: String? = null
)
