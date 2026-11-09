package com.momento.data.remote

import com.momento.core.common.AppError
import com.momento.core.common.AppResult
import com.momento.core.common.map
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
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private inline fun <T> apiCall(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: IOException) {
    AppResult.Failure(AppError(e.message ?: "Network error", cause = e, isNetworkError = true))
} catch (e: Exception) {
    AppResult.Failure(AppError(e.message ?: "Unexpected error", cause = e))
}

@Singleton
class RemoteFeedRepository @Inject constructor(
    private val api: MomentoApi
) : FeedRepository {

    override suspend fun getForYouFeed(page: Int): AppResult<List<Post>> =
        apiCall { api.forYouFeed(page) }.map { pageDto -> pageDto.items.map { it.toDomain() } }

    override suspend fun getFollowingFeed(page: Int): AppResult<List<Post>> =
        apiCall { api.followingFeed(page) }.map { pageDto -> pageDto.items.map { it.toDomain() } }

    override suspend fun toggleLike(postId: String, liked: Boolean): AppResult<Unit> =
        apiCall { if (liked) api.likePost(postId) else api.unlikePost(postId) }

    override suspend fun toggleBookmark(postId: String, bookmarked: Boolean): AppResult<Unit> =
        apiCall { if (bookmarked) api.bookmarkPost(postId) else api.unbookmarkPost(postId) }
}

@Singleton
class RemoteStoryRepository @Inject constructor(
    private val api: MomentoApi
) : StoryRepository {

    override suspend fun getStories(): AppResult<List<Story>> =
        apiCall { api.storiesFeed() }.map { pageDto -> pageDto.items.map { it.toDomain() } }
}

@Singleton
class RemoteProfileRepository @Inject constructor(
    private val api: MomentoApi
) : ProfileRepository {

    override suspend fun getProfile(userId: String): AppResult<UserProfile> =
        apiCall { api.user(userId) }.map { it.toDomain() }

    override suspend fun getMoments(userId: String): AppResult<List<Moment>> =
        apiCall { api.userMoments(userId, 0) }.map { pageDto -> pageDto.items.map { it.toDomain() } }

    override suspend fun getPosts(userId: String): AppResult<List<Post>> =
        apiCall { api.userPosts(userId, 0) }.map { pageDto -> pageDto.items.map { it.toDomain() } }

    override suspend fun follow(userId: String): AppResult<Unit> = apiCall { api.follow(userId) }

    override suspend fun unfollow(userId: String): AppResult<Unit> = apiCall { api.unfollow(userId) }
}

@Singleton
class RemoteDiscoverRepository @Inject constructor(
    private val api: MomentoApi
) : DiscoverRepository {

    override suspend fun getNearbyProfiles(): AppResult<List<NearbyProfile>> =
        apiCall { api.nearbyPeople() }.map { pageDto -> pageDto.items.map { it.toDomain() } }

    override suspend fun getTrendingSparks(filter: SparkFilter): AppResult<List<SparkCard>> =
        apiCall { api.trending(filter.name.lowercase()) }.map { pageDto -> pageDto.items.map { it.toDomain() } }

    override suspend fun getAudioRooms(): AppResult<List<AudioRoom>> =
        apiCall { api.audioRooms() }.map { pageDto -> pageDto.items.map { it.toDomain() } }

    override suspend fun pass(profileId: String): AppResult<Unit> =
        apiCall { api.pass(ActionBodyDto(profileId)) }

    override suspend fun like(profileId: String): AppResult<Unit> =
        apiCall { api.like(ActionBodyDto(profileId)) }

    override suspend fun superLike(profileId: String): AppResult<Unit> =
        apiCall { api.superLike(ActionBodyDto(profileId)) }
}

@Singleton
class RemotePodRepository @Inject constructor(
    private val api: MomentoApi
) : PodRepository {

    override suspend fun getPodFeed(): AppResult<List<PodFeedItem>> = apiCall {
        val feed = api.podFeed()
        buildList {
            addAll(feed.polls.map { it.toDomain() })
            addAll(feed.liveAudio.map { it.toDomain() })
            addAll(feed.mediaReels.map { it.toDomain() })
            addAll(feed.editorials.map { it.toDomain() })
            addAll(feed.soundtracks.map { it.toDomain() })
            addAll(feed.essays.map { it.toDomain() })
        }
    }

    override suspend fun getTopics(): AppResult<List<PodTopic>> =
        apiCall { api.podTopics() }.map { pageDto -> pageDto.items.map { it.toDomain() } }

    override suspend fun getLiveListenerCount(): AppResult<Long> =
        apiCall { api.liveListenerCount() }.map { it.count }

    override suspend fun vote(podId: String, pollId: String, optionId: String): AppResult<PodPollCard> =
        apiCall { api.votePoll(podId, pollId, VoteBodyDto(optionId)) }.map { it.toDomain() }

    override suspend fun joinPod(podId: String): AppResult<Unit> = apiCall { api.joinPod(podId) }
}
