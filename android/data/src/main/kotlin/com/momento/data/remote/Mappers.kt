package com.momento.data.remote

import com.momento.domain.model.AudioRoom
import com.momento.domain.model.EditorialDispatchCard
import com.momento.domain.model.EssayCard
import com.momento.domain.model.LiveAudioPodCard
import com.momento.domain.model.MediaReelCard
import com.momento.domain.model.MediaType
import com.momento.domain.model.Moment
import com.momento.domain.model.NearbyProfile
import com.momento.domain.model.PodPollCard
import com.momento.domain.model.PodPollOption
import com.momento.domain.model.PodTopic
import com.momento.domain.model.Post
import com.momento.domain.model.SoundtrackCard
import com.momento.domain.model.SparkCard
import com.momento.domain.model.Story
import com.momento.domain.model.StoryStatus
import com.momento.domain.model.UserProfile

internal fun UserDto.toDomain() = UserProfile(
    id = id,
    displayName = displayName,
    username = username,
    avatarUrl = avatarUrl,
    verified = verified,
    followerCount = followerCount,
    followingCount = followingCount,
    bio = bio,
    category = category,
    location = location,
    online = online
)

internal fun PostDto.toDomain() = Post(
    id = id,
    author = author.toDomain(),
    mediaUrl = mediaUrl,
    mediaType = if (mediaType == "VIDEO") MediaType.VIDEO else MediaType.IMAGE,
    caption = caption,
    likeCount = likeCount,
    commentCount = commentCount,
    shareCount = shareCount,
    isLiked = isLiked,
    isBookmarked = isBookmarked,
    createdAtMillis = createdAtMillis,
    mediaAspectRatio = mediaAspectRatio,
    videoDurationSeconds = videoDurationSeconds
)

internal fun StoryDto.toDomain() = Story(
    id = id,
    author = author.toDomain(),
    mediaUrl = mediaUrl,
    status = StoryStatus.valueOf(status),
    liveViewerCount = liveViewerCount,
    isViewed = isViewed
)

internal fun MomentDto.toDomain() = Moment(
    id = id,
    author = author.toDomain(),
    thumbnailUrl = thumbnailUrl,
    viewCount = viewCount,
    durationSeconds = durationSeconds
)

internal fun NearbyProfileDto.toDomain() = NearbyProfile(
    profile = profile.toDomain(),
    age = age,
    distanceKm = distanceKm,
    matchPercent = matchPercent,
    interests = interests
)

internal fun SparkCardDto.toDomain() = SparkCard(
    id = id,
    title = title,
    author = author.toDomain(),
    mediaUrl = mediaUrl,
    viewCount = viewCount,
    likeCount = likeCount,
    commentCount = commentCount,
    durationSeconds = durationSeconds
)

internal fun AudioRoomDto.toDomain() = AudioRoom(
    id = id,
    title = title,
    listenerCount = listenerCount,
    hosts = hosts.map { it.toDomain() },
    artworkUrl = artworkUrl
)

internal fun PodTopicDto.toDomain() = PodTopic(
    id = id,
    name = name,
    participantCount = participantCount,
    live = live
)

internal fun PodPollCardDto.toDomain() = PodPollCard(
    id = id,
    podId = podId,
    author = author.toDomain(),
    question = question,
    description = description,
    endsInLabel = endsInLabel,
    totalVotes = totalVotes,
    replyCount = replyCount,
    options = options.map { PodPollOption(it.id, it.text, it.voteCount, it.percentage) },
    myVoteOptionId = myVoteOptionId
)

internal fun LiveAudioPodCardDto.toDomain() = LiveAudioPodCard(
    id = id,
    title = title,
    hosts = hosts.map { it.toDomain() },
    listenerCount = listenerCount,
    positionSeconds = positionSeconds,
    durationSeconds = durationSeconds
)

internal fun MediaReelCardDto.toDomain() = MediaReelCard(
    id = id,
    author = author.toDomain(),
    thumbnailUrl = thumbnailUrl,
    viewCount = viewCount,
    caption = caption,
    hashtag = hashtag,
    likeCount = likeCount,
    commentCount = commentCount,
    shareCount = shareCount
)

internal fun EditorialDispatchCardDto.toDomain() = EditorialDispatchCard(
    id = id,
    title = title,
    snippet = snippet,
    domain = domain,
    authorName = authorName,
    readMinutes = readMinutes,
    footnoteCount = footnoteCount,
    participantCount = participantCount
)

internal fun SoundtrackCardDto.toDomain() = SoundtrackCard(
    id = id,
    title = title,
    artist = artist,
    artworkUrl = artworkUrl,
    rank = rank,
    clipCount = clipCount
)

internal fun EssayCardDto.toDomain() = EssayCard(
    id = id,
    author = author.toDomain(),
    postedAtLabel = postedAtLabel,
    title = title,
    preview = preview,
    likeCount = likeCount,
    commentCount = commentCount,
    shareCount = shareCount
)
