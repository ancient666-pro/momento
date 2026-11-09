package com.momento.backend.database

import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Exposed table definitions mirroring the Flyway migrations. Columns that are
 * not needed by any query are intentionally left unmapped so schema evolution
 * (adding columns) never breaks the service.
 */

object Users : Table("users") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val email = text("email")
    val username = text("username")
    val displayName = text("display_name")
    val avatarUrl = text("avatar_url").nullable()
    val verified = bool("verified").default(false)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
    val deletedAt = timestamp("deleted_at").nullable()
    override val primaryKey = PrimaryKey(id)
}

object UserCredentials : Table("user_credentials") {
    val userId = uuid("user_id").references(Users.id)
    val passwordHash = text("password_hash")
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(userId)
}

object UserProfiles : Table("user_profiles") {
    val userId = uuid("user_id").references(Users.id)
    val bio = text("bio").default("")
    val category = text("category").nullable()
    val location = text("location").nullable()
    val website = text("website").nullable()
    val isPrivate = bool("is_private").default(false)
    val age = integer("age").nullable()
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(userId)
}

object RefreshTokens : Table("refresh_tokens") {
    val jti = uuid("jti")
    val userId = uuid("user_id").references(Users.id)
    val deviceId = text("device_id").nullable()
    val expiresAt = timestamp("expires_at")
    val revokedAt = timestamp("revoked_at").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(jti)
}

object Follows : Table("follows") {
    val followerId = uuid("follower_id").references(Users.id)
    val followeeId = uuid("followee_id").references(Users.id)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(followerId, followeeId)
}

object Blocks : Table("blocks") {
    val blockerId = uuid("blocker_id").references(Users.id)
    val blockedId = uuid("blocked_id").references(Users.id)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(blockerId, blockedId)
}

object Mutes : Table("mutes") {
    val muterId = uuid("muter_id").references(Users.id)
    val mutedId = uuid("muted_id").references(Users.id)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(muterId, mutedId)
}

object Posts : Table("posts") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val authorId = uuid("author_id").references(Users.id)
    val caption = text("caption").default("")
    val visibility = text("visibility").default("public")
    val kind = text("kind").default("post")
    val hashtag = text("hashtag").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
    val deletedAt = timestamp("deleted_at").nullable()
    override val primaryKey = PrimaryKey(id)
}

object PostMedia : Table("post_media") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val postId = uuid("post_id").references(Posts.id)
    val mediaAssetId = uuid("media_asset_id")
    val mediaType = text("media_type").default("image")
    val position = integer("position").default(0)
    val aspectRatio = float("aspect_ratio").nullable()
    val durationSeconds = integer("duration_seconds").nullable()
    override val primaryKey = PrimaryKey(id)
}

object PostLikes : Table("post_likes") {
    val postId = uuid("post_id").references(Posts.id)
    val userId = uuid("user_id").references(Users.id)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(postId, userId)
}

object Comments : Table("comments") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val postId = uuid("post_id").references(Posts.id)
    val authorId = uuid("author_id").references(Users.id)
    val parentId = uuid("parent_id").nullable()
    val body = text("body")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val deletedAt = timestamp("deleted_at").nullable()
    override val primaryKey = PrimaryKey(id)
}

object PostBookmarks : Table("post_bookmarks") {
    val postId = uuid("post_id").references(Posts.id)
    val userId = uuid("user_id").references(Users.id)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(postId, userId)
}

object PostShares : Table("post_shares") {
    val postId = uuid("post_id").references(Posts.id)
    val userId = uuid("user_id").references(Users.id)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}

object Hashtags : Table("hashtags") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val name = text("name")
    val postCount = long("post_count").default(0)
    override val primaryKey = PrimaryKey(id)
}

object PostHashtags : Table("post_hashtags") {
    val postId = uuid("post_id").references(Posts.id)
    val hashtagId = uuid("hashtag_id").references(Hashtags.id)
    override val primaryKey = PrimaryKey(postId, hashtagId)
}

object Stories : Table("stories") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val authorId = uuid("author_id").references(Users.id)
    val mediaAssetId = uuid("media_asset_id")
    val mediaType = text("media_type").default("image")
    val caption = text("caption").default("")
    val liveViewerCount = long("live_viewer_count").default(0)
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}

object StoryViews : Table("story_views") {
    val storyId = uuid("story_id").references(Stories.id)
    val userId = uuid("user_id").references(Users.id)
    val viewedAt = timestamp("viewed_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(storyId, userId)
}

object StoryReactions : Table("story_reactions") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val storyId = uuid("story_id").references(Stories.id)
    val userId = uuid("user_id").references(Users.id)
    val reaction = text("reaction")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}

object Moments : Table("moments") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val authorId = uuid("author_id").references(Users.id)
    val mediaAssetId = uuid("media_asset_id")
    val caption = text("caption").default("")
    val thumbnailUrl = text("thumbnail_url").nullable()
    val viewCount = long("view_count").default(0)
    val durationSeconds = integer("duration_seconds").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}

object DiscoveryActions : Table("discovery_actions") {
    val actorId = uuid("actor_id").references(Users.id)
    val targetId = uuid("target_id").references(Users.id)
    val action = text("action")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(actorId, targetId, action)
}

object UserLocationData : Table("user_location_data") {
    val userId = uuid("user_id").references(Users.id)
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()
    val privacyBandKm = integer("privacy_band_km").default(5)
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(userId)
}

object Pods : Table("pods") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val hostId = uuid("host_id").references(Users.id)
    val title = text("title")
    val description = text("description").default("")
    val topicHashtag = text("topic_hashtag").nullable()
    val visibility = text("visibility").default("public")
    val artworkUrl = text("artwork_url").nullable()
    val live = bool("live").default(false)
    val listenerCount = long("listener_count").default(0)
    val audioOnly = bool("audio_only").default(true)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val deletedAt = timestamp("deleted_at").nullable()
    override val primaryKey = PrimaryKey(id)
}

object PodMembers : Table("pod_members") {
    val podId = uuid("pod_id").references(Pods.id)
    val userId = uuid("user_id").references(Users.id)
    val role = text("role").default("listener")
    val joinedAt = timestamp("joined_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(podId, userId)
}

object PodSessions : Table("pod_sessions") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val podId = uuid("pod_id").references(Pods.id)
    val nexconnChannelUrl = text("nexconn_channel_url").nullable()
    val startedAt = timestamp("started_at").clientDefault { Instant.now() }
    val endedAt = timestamp("ended_at").nullable()
    override val primaryKey = PrimaryKey(id)
}

object PodPolls : Table("pod_polls") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val podId = uuid("pod_id").references(Pods.id)
    val authorId = uuid("author_id").references(Users.id)
    val question = text("question")
    val description = text("description").default("")
    val replyCount = long("reply_count").default(0)
    val endsAt = timestamp("ends_at").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}

object PollOptions : Table("poll_options") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val pollId = uuid("poll_id").references(PodPolls.id)
    val text = text("text")
    val position = integer("position").default(0)
    override val primaryKey = PrimaryKey(id)
}

object PollVotes : Table("poll_votes") {
    val pollId = uuid("poll_id").references(PodPolls.id)
    val optionId = uuid("option_id").references(PollOptions.id)
    val userId = uuid("user_id").references(Users.id)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(pollId, userId)
}

object PodTopics : Table("pod_topics") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val name = text("name")
    val participantCount = long("participant_count").default(0)
    val live = bool("live").default(false)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}

object MediaAssets : Table("media_assets") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val ownerId = uuid("owner_id").references(Users.id)
    val bucket = text("bucket")
    val objectKey = text("object_key")
    val url = text("url").nullable()
    val mimeType = text("mime_type")
    val sizeBytes = long("size_bytes").default(0)
    val width = integer("width").nullable()
    val height = integer("height").nullable()
    val status = text("status").default("pending")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}

object NexconnUserMappings : Table("nexconn_user_mappings") {
    val userId = uuid("user_id").references(Users.id)
    val nexconnUserId = text("nexconn_user_id")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(userId)
}

object NexconnChannelMappings : Table("nexconn_channel_mappings") {
    val channelUrl = text("channel_url")
    val kind = text("kind")
    val podId = uuid("pod_id").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(channelUrl)
}

object CallHistory : Table("call_history") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val callerId = uuid("caller_id").references(Users.id)
    val calleeId = uuid("callee_id").references(Users.id).nullable()
    val kind = text("kind")
    val status = text("status")
    val durationSeconds = integer("duration_seconds").nullable()
    val startedAt = timestamp("started_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}

object CommunicationEvents : Table("communication_events") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val eventType = text("event_type")
    val payload = text("payload")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}

object WebhookEvents : Table("webhook_events") {
    val eventId = text("event_id")
    val webhookSource = text("source")
    val receivedAt = timestamp("received_at").clientDefault { Instant.now() }
    val processedAt = timestamp("processed_at").nullable()
    val attempts = integer("attempts").default(0)
    val status = text("status").default("received")
    override val primaryKey = PrimaryKey(eventId)
}

object Notifications : Table("notifications") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val userId = uuid("user_id").references(Users.id)
    val kind = text("kind")
    val payload = text("payload").default("{}")
    val readAt = timestamp("read_at").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}

object NotificationPreferences : Table("notification_preferences") {
    val userId = uuid("user_id").references(Users.id)
    val newFollower = bool("new_follower").default(true)
    val likes = bool("likes").default(true)
    val comments = bool("comments").default(true)
    val mentions = bool("mentions").default(true)
    val directMessages = bool("direct_messages").default(true)
    val calls = bool("calls").default(true)
    val podActivity = bool("pod_activity").default(true)
    override val primaryKey = PrimaryKey(userId)
}

object DeviceTokens : Table("device_tokens") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val userId = uuid("user_id").references(Users.id)
    val fcmToken = text("fcm_token")
    val deviceName = text("device_name").nullable()
    val lastSeenAt = timestamp("last_seen_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}

object UserReports : Table("user_reports") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val reporterId = uuid("reporter_id").references(Users.id)
    val targetUserId = uuid("target_user_id").references(Users.id).nullable()
    val reason = text("reason")
    val details = text("details").nullable()
    val status = text("status").default("open")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}

object ContentReports : Table("content_reports") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val reporterId = uuid("reporter_id").references(Users.id)
    val targetType = text("target_type")
    val targetId = uuid("target_id")
    val reason = text("reason")
    val details = text("details").nullable()
    val status = text("status").default("open")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}

object EditorialDispatches : Table("editorial_dispatches") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val title = text("title")
    val snippet = text("snippet")
    val domain = text("domain")
    val authorName = text("author_name")
    val readMinutes = integer("read_minutes").default(5)
    val footnoteCount = long("footnote_count").default(0)
    val participantCount = long("participant_count").default(0)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}

object Soundtracks : Table("soundtracks") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val title = text("title")
    val artist = text("artist")
    val artworkUrl = text("artwork_url")
    val rank = integer("rank").default(1)
    val clipCount = long("clip_count").default(0)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}

object Essays : Table("essays") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val authorId = uuid("author_id").references(Users.id)
    val title = text("title")
    val preview = text("preview").default("")
    val likeCount = long("like_count").default(0)
    val commentCount = long("comment_count").default(0)
    val shareCount = long("share_count").default(0)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}
