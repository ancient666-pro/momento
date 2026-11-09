package com.momento.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class AuthTokensDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long = 0
)

@Serializable
data class AuthResponseDto(
    val user: UserDto,
    val tokens: AuthTokensDto
)

@Serializable
data class PageDto<T>(
    val items: List<T> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null
)

@Serializable
data class UserDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String,
    val verified: Boolean = false,
    @SerialName("follower_count") val followerCount: Long = 0,
    @SerialName("following_count") val followingCount: Long = 0,
    val bio: String = "",
    val category: String? = null,
    val location: String? = null,
    val online: Boolean = false
)

@Serializable
data class PostDto(
    val id: String,
    val author: UserDto,
    @SerialName("media_url") val mediaUrl: String,
    @SerialName("media_type") val mediaType: String = "IMAGE",
    val caption: String = "",
    @SerialName("like_count") val likeCount: Long = 0,
    @SerialName("comment_count") val commentCount: Long = 0,
    @SerialName("share_count") val shareCount: Long = 0,
    @SerialName("is_liked") val isLiked: Boolean = false,
    @SerialName("is_bookmarked") val isBookmarked: Boolean = false,
    @SerialName("created_at") val createdAtMillis: Long = 0,
    @SerialName("media_aspect_ratio") val mediaAspectRatio: Float = 1f,
    @SerialName("video_duration_seconds") val videoDurationSeconds: Int? = null
)

@Serializable
data class StoryDto(
    val id: String,
    val author: UserDto,
    @SerialName("media_url") val mediaUrl: String,
    val status: String = "NONE",
    @SerialName("live_viewer_count") val liveViewerCount: Long = 0,
    @SerialName("is_viewed") val isViewed: Boolean = false
)

@Serializable
data class MomentDto(
    val id: String,
    val author: UserDto,
    @SerialName("thumbnail_url") val thumbnailUrl: String,
    @SerialName("view_count") val viewCount: Long = 0,
    @SerialName("duration_seconds") val durationSeconds: Int = 0
)

@Serializable
data class NearbyProfileDto(
    val profile: UserDto,
    val age: Int? = null,
    @SerialName("distance_km") val distanceKm: Double = 0.0,
    @SerialName("match_percent") val matchPercent: Int = 0,
    val interests: List<String> = emptyList()
)

@Serializable
data class SparkCardDto(
    val id: String,
    val title: String,
    val author: UserDto,
    @SerialName("media_url") val mediaUrl: String,
    @SerialName("view_count") val viewCount: Long = 0,
    @SerialName("like_count") val likeCount: Long = 0,
    @SerialName("comment_count") val commentCount: Long = 0,
    @SerialName("duration_seconds") val durationSeconds: Int? = null
)

@Serializable
data class AudioRoomDto(
    val id: String,
    val title: String,
    @SerialName("listener_count") val listenerCount: Long = 0,
    val hosts: List<UserDto> = emptyList(),
    @SerialName("artwork_url") val artworkUrl: String? = null
)

@Serializable
data class PodTopicDto(
    val id: String,
    val name: String,
    @SerialName("participant_count") val participantCount: Long = 0,
    val live: Boolean = false
)

@Serializable
data class PodPollOptionDto(
    val id: String,
    val text: String,
    @SerialName("vote_count") val voteCount: Long,
    val percentage: Int
)

@Serializable
data class PodPollCardDto(
    val id: String,
    @SerialName("pod_id") val podId: String,
    val author: UserDto,
    val question: String,
    val description: String,
    @SerialName("ends_in") val endsInLabel: String,
    @SerialName("total_votes") val totalVotes: Long,
    @SerialName("reply_count") val replyCount: Long,
    val options: List<PodPollOptionDto>,
    @SerialName("my_vote_option_id") val myVoteOptionId: String? = null
)

@Serializable
data class LiveAudioPodCardDto(
    val id: String,
    val title: String,
    val hosts: List<UserDto>,
    @SerialName("listener_count") val listenerCount: Long,
    @SerialName("position_seconds") val positionSeconds: Int,
    @SerialName("duration_seconds") val durationSeconds: Int
)

@Serializable
data class MediaReelCardDto(
    val id: String,
    val author: UserDto,
    @SerialName("thumbnail_url") val thumbnailUrl: String,
    @SerialName("view_count") val viewCount: Long,
    val caption: String,
    val hashtag: String,
    @SerialName("like_count") val likeCount: Long,
    @SerialName("comment_count") val commentCount: Long,
    @SerialName("share_count") val shareCount: Long
)

@Serializable
data class EditorialDispatchCardDto(
    val id: String,
    val title: String,
    val snippet: String,
    val domain: String,
    @SerialName("author_name") val authorName: String,
    @SerialName("read_minutes") val readMinutes: Int,
    @SerialName("footnote_count") val footnoteCount: Long,
    @SerialName("participant_count") val participantCount: Long
)

@Serializable
data class SoundtrackCardDto(
    val id: String,
    val title: String,
    val artist: String,
    @SerialName("artwork_url") val artworkUrl: String,
    val rank: Int,
    @SerialName("clip_count") val clipCount: Long
)

@Serializable
data class NexconnTokenDto(
    @SerialName("user_id") val userId: String,
    @SerialName("access_token") val accessToken: String,
    @SerialName("app_key") val appKey: String? = null,
    @SerialName("area_code") val areaCode: String? = null,
    @SerialName("api_base_url") val apiBaseUrl: String? = null
)

@Serializable
data class EssayCardDto(
    val id: String,
    val author: UserDto,
    @SerialName("posted_at") val postedAtLabel: String,
    val title: String,
    val preview: String,
    @SerialName("like_count") val likeCount: Long,
    @SerialName("comment_count") val commentCount: Long,
    @SerialName("share_count") val shareCount: Long
)
