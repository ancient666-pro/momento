package com.momento.domain.model

data class PodTopic(
    val id: String,
    val name: String,
    val participantCount: Long = 0,
    val live: Boolean = false
)

sealed interface PodFeedItem {
    val id: String
}

data class PodPollCard(
    override val id: String,
    val podId: String,
    val author: UserProfile,
    val question: String,
    val description: String,
    val endsInLabel: String,
    val totalVotes: Long,
    val replyCount: Long,
    val options: List<PodPollOption>,
    val myVoteOptionId: String? = null
) : PodFeedItem

data class PodPollOption(
    val id: String,
    val text: String,
    val voteCount: Long,
    val percentage: Int
)

data class LiveAudioPodCard(
    override val id: String,
    val title: String,
    val hosts: List<UserProfile>,
    val listenerCount: Long,
    val positionSeconds: Int,
    val durationSeconds: Int
) : PodFeedItem

data class MediaReelCard(
    override val id: String,
    val author: UserProfile,
    val thumbnailUrl: String,
    val viewCount: Long,
    val caption: String,
    val hashtag: String,
    val likeCount: Long,
    val commentCount: Long,
    val shareCount: Long
) : PodFeedItem

data class EditorialDispatchCard(
    override val id: String,
    val title: String,
    val snippet: String,
    val domain: String,
    val authorName: String,
    val readMinutes: Int,
    val footnoteCount: Long,
    val participantCount: Long
) : PodFeedItem

data class SoundtrackCard(
    override val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val rank: Int,
    val clipCount: Long
) : PodFeedItem

data class EssayCard(
    override val id: String,
    val author: UserProfile,
    val postedAtLabel: String,
    val title: String,
    val preview: String,
    val likeCount: Long,
    val commentCount: Long,
    val shareCount: Long
) : PodFeedItem
