package com.momento.domain.model

enum class MediaType { IMAGE, VIDEO }

data class Post(
    val id: String,
    val author: UserProfile,
    val mediaUrl: String,
    val mediaType: MediaType,
    val caption: String = "",
    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val shareCount: Long = 0,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val createdAtMillis: Long = 0,
    val mediaAspectRatio: Float = 1f,
    val videoDurationSeconds: Int? = null
)

data class Story(
    val id: String,
    val author: UserProfile,
    val mediaUrl: String,
    val status: StoryStatus = StoryStatus.NONE,
    val liveViewerCount: Long = 0,
    val isViewed: Boolean = false
)

enum class StoryStatus { NONE, LIVE, PREMIERE }

data class Moment(
    val id: String,
    val author: UserProfile,
    val thumbnailUrl: String,
    val viewCount: Long = 0,
    val durationSeconds: Int = 0
)
