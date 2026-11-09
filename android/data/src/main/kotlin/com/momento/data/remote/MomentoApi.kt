package com.momento.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class PodFeedDto(
    val polls: List<PodPollCardDto> = emptyList(),
    val liveAudio: List<LiveAudioPodCardDto> = emptyList(),
    val mediaReels: List<MediaReelCardDto> = emptyList(),
    val editorials: List<EditorialDispatchCardDto> = emptyList(),
    val soundtracks: List<SoundtrackCardDto> = emptyList(),
    val essays: List<EssayCardDto> = emptyList()
)

@Serializable
data class ActionBodyDto(
    val target: String? = null
)

@Serializable
data class VoteBodyDto(
    val optionId: String
)

@Serializable
data class LiveCountDto(
    val count: Long = 0
)

interface MomentoApi {

    // Authentication
    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequestDto): AuthResponseDto

    // Feed
    @GET("api/v1/feed/for-you")
    suspend fun forYouFeed(@Query("page") page: Int): PageDto<PostDto>

    @GET("api/v1/feed/following")
    suspend fun followingFeed(@Query("page") page: Int): PageDto<PostDto>

    @POST("api/v1/posts/{postId}/like")
    suspend fun likePost(@Path("postId") postId: String)

    @DELETE("api/v1/posts/{postId}/like")
    suspend fun unlikePost(@Path("postId") postId: String)

    @POST("api/v1/posts/{postId}/bookmark")
    suspend fun bookmarkPost(@Path("postId") postId: String)

    @DELETE("api/v1/posts/{postId}/bookmark")
    suspend fun unbookmarkPost(@Path("postId") postId: String)

    // Stories
    @GET("api/v1/stories/feed")
    suspend fun storiesFeed(): PageDto<StoryDto>

    // Profiles
    @GET("api/v1/users/{userId}")
    suspend fun user(@Path("userId") userId: String): UserDto

    @GET("api/v1/users/{userId}/moments")
    suspend fun userMoments(@Path("userId") userId: String, @Query("page") page: Int): PageDto<MomentDto>

    @GET("api/v1/users/{userId}/posts")
    suspend fun userPosts(@Path("userId") userId: String, @Query("page") page: Int): PageDto<PostDto>

    @POST("api/v1/users/{userId}/follow")
    suspend fun follow(@Path("userId") userId: String)

    @DELETE("api/v1/users/{userId}/follow")
    suspend fun unfollow(@Path("userId") userId: String)

    // Discover
    @GET("api/v1/discover/people")
    suspend fun nearbyPeople(): PageDto<NearbyProfileDto>

    @GET("api/v1/discover/trending")
    suspend fun trending(@Query("filter") filter: String): PageDto<SparkCardDto>

    @GET("api/v1/discover/audio-rooms")
    suspend fun audioRooms(): PageDto<AudioRoomDto>

    @POST("api/v1/discover/pass")
    suspend fun pass(@Body body: ActionBodyDto)

    @POST("api/v1/discover/like")
    suspend fun like(@Body body: ActionBodyDto)

    @POST("api/v1/discover/super-like")
    suspend fun superLike(@Body body: ActionBodyDto)

    // Pod
    @GET("api/v1/pods/feed")
    suspend fun podFeed(): PodFeedDto

    @GET("api/v1/pods/topics")
    suspend fun podTopics(): PageDto<PodTopicDto>

    @GET("api/v1/pods/live-count")
    suspend fun liveListenerCount(): LiveCountDto

    @POST("api/v1/pods/{podId}/polls/{pollId}/vote")
    suspend fun votePoll(
        @Path("podId") podId: String,
        @Path("pollId") pollId: String,
        @Body body: VoteBodyDto
    ): PodPollCardDto

    @POST("api/v1/pods/{podId}/join")
    suspend fun joinPod(@Path("podId") podId: String)

    // Real-time communications (Nexconn). The server signs the request with the
    // App Secret and returns a short-lived SDK access token plus the public App Key.
    @POST("api/v1/communications/nexconn/token")
    suspend fun nexconnToken(): NexconnTokenDto
}
