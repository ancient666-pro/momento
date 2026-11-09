package com.momento.data.preview

import com.momento.core.common.AppResult
import com.momento.domain.model.AudioRoom
import com.momento.domain.model.Moment
import com.momento.domain.model.NearbyProfile
import com.momento.domain.model.PodFeedItem
import com.momento.domain.model.PodPollCard
import com.momento.domain.model.PodTopic
import com.momento.domain.model.Post
import com.momento.domain.model.SparkCard
import com.momento.domain.model.Story
import com.momento.domain.model.UserProfile
import com.momento.domain.repository.DiscoverRepository
import com.momento.domain.repository.FeedRepository
import com.momento.domain.repository.PodRepository
import com.momento.domain.repository.ProfileRepository
import com.momento.domain.repository.SparkFilter
import com.momento.domain.repository.StoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private const val SIMULATED_LATENCY_MS = 350L

private suspend fun <T> preview(block: () -> T): AppResult<T> {
    delay(SIMULATED_LATENCY_MS)
    return AppResult.Success(block())
}

@Singleton
class DebugFeedRepository @Inject constructor() : FeedRepository {

    private val posts = DesignPreviewData.homePosts.toMutableList()
    private val following = DesignPreviewData.followingPosts.toMutableList()

    override suspend fun getForYouFeed(page: Int): AppResult<List<Post>> =
        preview { posts.toList() }

    override suspend fun getFollowingFeed(page: Int): AppResult<List<Post>> =
        preview { following.toList() }

    override suspend fun toggleLike(postId: String, liked: Boolean): AppResult<Unit> = preview {
        posts.replaceAll { if (it.id == postId) it.copy(isLiked = liked, likeCount = it.likeCount + if (liked) 1 else -1) else it }
        following.replaceAll { if (it.id == postId) it.copy(isLiked = liked, likeCount = it.likeCount + if (liked) 1 else -1) else it }
    }

    override suspend fun toggleBookmark(postId: String, bookmarked: Boolean): AppResult<Unit> = preview {
        posts.replaceAll { if (it.id == postId) it.copy(isBookmarked = bookmarked) else it }
        following.replaceAll { if (it.id == postId) it.copy(isBookmarked = bookmarked) else it }
    }
}

@Singleton
class DebugStoryRepository @Inject constructor() : StoryRepository {
    override suspend fun getStories(): AppResult<List<Story>> =
        preview { DesignPreviewData.stories }
}

@Singleton
class DebugProfileRepository @Inject constructor() : ProfileRepository {

    private var followed = false

    override suspend fun getProfile(userId: String): AppResult<UserProfile> = preview {
        when (userId) {
            "me", DesignPreviewData.leslie.id -> DesignPreviewData.leslie
            DesignPreviewData.elena.id -> DesignPreviewData.elena
            else -> DesignPreviewData.leslie
        }
    }

    override suspend fun getMoments(userId: String): AppResult<List<Moment>> =
        preview { DesignPreviewData.moments }

    override suspend fun getPosts(userId: String): AppResult<List<Post>> =
        preview { DesignPreviewData.profilePosts }

    override suspend fun follow(userId: String): AppResult<Unit> = preview { followed = true }

    override suspend fun unfollow(userId: String): AppResult<Unit> = preview { followed = false }
}

@Singleton
class DebugDiscoverRepository @Inject constructor() : DiscoverRepository {

    private var nearby = DesignPreviewData.nearbyProfiles.toMutableList()

    override suspend fun getNearbyProfiles(): AppResult<List<NearbyProfile>> =
        preview { nearby.toList() }

    override suspend fun getTrendingSparks(filter: SparkFilter): AppResult<List<SparkCard>> =
        preview { DesignPreviewData.sparksFor(filter.name) }

    override suspend fun getAudioRooms(): AppResult<List<AudioRoom>> =
        preview { DesignPreviewData.audioRooms }

    override suspend fun pass(profileId: String): AppResult<Unit> = preview {
        nearby.removeAll { it.profile.id == profileId }
    }

    override suspend fun like(profileId: String): AppResult<Unit> = preview {
        nearby.removeAll { it.profile.id == profileId }
    }

    override suspend fun superLike(profileId: String): AppResult<Unit> = preview {
        nearby.removeAll { it.profile.id == profileId }
    }
}

@Singleton
class DebugPodRepository @Inject constructor() : PodRepository {

    private var feed = DesignPreviewData.podFeed.toMutableList()
    private var voted = false

    override suspend fun getPodFeed(): AppResult<List<PodFeedItem>> =
        preview { feed.toList() }

    override suspend fun getTopics(): AppResult<List<PodTopic>> =
        preview { DesignPreviewData.podTopics }

    override suspend fun getLiveListenerCount(): AppResult<Long> =
        preview { 14_800L }

    override suspend fun vote(podId: String, pollId: String, optionId: String): AppResult<PodPollCard> = preview {
        val index = feed.indexOfFirst { it is PodPollCard && it.id == pollId }
        val poll = feed[index] as PodPollCard
        if (voted) {
            poll
        } else {
            voted = true
            val updated = recalculate(poll, optionId)
            feed[index] = updated
            updated
        }
    }

    override suspend fun joinPod(podId: String): AppResult<Unit> = preview { }

    private fun recalculate(poll: PodPollCard, chosenOptionId: String): PodPollCard {
        val newTotal = poll.totalVotes + 1
        return poll.copy(
            totalVotes = newTotal,
            myVoteOptionId = chosenOptionId,
            options = poll.options.map { option ->
                val gained = if (option.id == chosenOptionId) 1L else 0L
                val votes = option.voteCount + gained
                option.copy(
                    voteCount = votes,
                    percentage = ((votes.toDouble() / newTotal) * 100).roundToInt()
                )
            }
        )
    }
}
