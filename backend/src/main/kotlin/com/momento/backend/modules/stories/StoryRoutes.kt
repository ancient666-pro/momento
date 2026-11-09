package com.momento.backend.modules.stories

import com.momento.backend.common.StoryDto
import com.momento.backend.common.badRequest
import com.momento.backend.common.currentUserIdOrNull
import com.momento.backend.common.forbidden
import com.momento.backend.common.notFound
import com.momento.backend.common.pathUuid
import com.momento.backend.common.requireUserId
import com.momento.backend.database.DatabaseFactory
import com.momento.backend.database.MediaAssets
import com.momento.backend.database.Stories
import com.momento.backend.database.StoryReactions
import com.momento.backend.database.StoryViews
import com.momento.backend.database.Users
import com.momento.backend.modules.users.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

@Serializable
data class CreateStoryRequest(
    val mediaUrl: String,
    val mediaType: String = "IMAGE",
    val caption: String = "",
    val status: String = "NONE"
)

@Serializable
data class StoryReactionRequest(val reaction: String)

fun Route.storiesModule() {
    route("/stories") {
        get("/feed") {
            val viewer = call.currentUserIdOrNull()
            val items = DatabaseFactory.query {
                val rows = Stories.selectAll()
                    .where { Stories.expiresAt greater Instant.now() }
                    .orderBy(Stories.createdAt, SortOrder.DESC)
                    .limit(40)
                    .toList()
                if (rows.isEmpty()) return@query emptyList<StoryDto>()
                val authors = UserService.dtosByIds(rows.map { it[Stories.authorId] })
                val assets = MediaAssets.selectAll()
                    .where { MediaAssets.id inList rows.map { it[Stories.mediaAssetId] } }
                    .associateBy { it[MediaAssets.id] }
                val viewed = if (viewer == null) emptySet() else StoryViews.select(StoryViews.storyId)
                    .where { StoryViews.userId eq viewer }
                    .map { it[StoryViews.storyId] }
                    .toSet()
                rows.mapNotNull { row ->
                    val id = row[Stories.id]
                    val asset = assets[row[Stories.mediaAssetId]]
                    val author = authors[row[Stories.authorId]] ?: return@mapNotNull null
                    StoryDto(
                        id = id.toString(),
                        author = author,
                        mediaUrl = asset?.get(MediaAssets.url) ?: "",
                        status = statusFor(row),
                        liveViewerCount = row[Stories.liveViewerCount],
                        isViewed = id in viewed
                    )
                }
            }
            call.respond(com.momento.backend.common.paged(items, 0))
        }

        get("/{storyId}") {
            val storyId = call.pathUuid("storyId")
            val viewer = call.currentUserIdOrNull()
            val dto = DatabaseFactory.query {
                val row = Stories.selectAll().where { Stories.id eq storyId }.firstOrNull()
                    ?: throw notFound("Story not found")
                val asset = MediaAssets.selectAll()
                    .where { MediaAssets.id eq row[Stories.mediaAssetId] }
                    .firstOrNull()
                StoryDto(
                    id = storyId.toString(),
                    author = UserService.dtoById(row[Stories.authorId]) ?: throw notFound("Author not found"),
                    mediaUrl = asset?.get(MediaAssets.url) ?: "",
                    status = statusFor(row),
                    liveViewerCount = row[Stories.liveViewerCount],
                    isViewed = viewer != null && StoryViews.selectAll()
                        .where { (StoryViews.storyId eq storyId) and (StoryViews.userId eq viewer) }
                        .count() > 0
                )
            }
            call.respond(dto)
        }

        post {
            val userId = call.requireUserId()
            val body = call.receive<CreateStoryRequest>()
            if (!body.mediaUrl.startsWith("http")) throw badRequest("Media URL must be an http(s) URL")
            val id = DatabaseFactory.query {
                val assetId = UUID.randomUUID()
                MediaAssets.insert {
                    it[MediaAssets.id] = assetId
                    it[MediaAssets.ownerId] = userId
                    it[MediaAssets.bucket] = "external"
                    it[MediaAssets.objectKey] = body.mediaUrl
                    it[MediaAssets.url] = body.mediaUrl
                    it[MediaAssets.mimeType] = if (body.mediaType.equals("VIDEO", true)) "video/mp4" else "image/jpeg"
                    it[MediaAssets.sizeBytes] = 0
                    it[MediaAssets.status] = "ready"
                }
                val storyId = UUID.randomUUID()
                Stories.insert {
                    it[Stories.id] = storyId
                    it[Stories.authorId] = userId
                    it[Stories.mediaAssetId] = assetId
                    it[Stories.mediaType] = body.mediaType.lowercase()
                    it[Stories.caption] = body.caption.take(200)
                    it[Stories.expiresAt] = Instant.now().plus(24, ChronoUnit.HOURS)
                    it[Stories.createdAt] = Instant.now()
                }
                storyId
            }
            call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
        }

        post("/{storyId}/view") {
            val viewer = call.requireUserId()
            val storyId = call.pathUuid("storyId")
            DatabaseFactory.query {
                if (Stories.selectAll().where { Stories.id eq storyId }.count() == 0L) {
                    throw notFound("Story not found")
                }
                val exists = StoryViews.selectAll()
                    .where { (StoryViews.storyId eq storyId) and (StoryViews.userId eq viewer) }
                    .count() > 0
                if (!exists) {
                    StoryViews.insert {
                        it[StoryViews.storyId] = storyId
                        it[StoryViews.userId] = viewer
                        it[viewedAt] = Instant.now()
                    }
                }
            }
            call.respond(HttpStatusCode.NoContent)
        }

        post("/{storyId}/reaction") {
            val viewer = call.requireUserId()
            val storyId = call.pathUuid("storyId")
            val body = call.receive<StoryReactionRequest>()
            if (body.reaction.isBlank()) throw badRequest("Reaction is required")
            DatabaseFactory.query {
                StoryReactions.insert {
                    it[StoryReactions.storyId] = storyId
                    it[StoryReactions.userId] = viewer
                    it[reaction] = body.reaction.take(40)
                    it[createdAt] = Instant.now()
                }
            }
            call.respond(HttpStatusCode.NoContent)
        }

        delete("/{storyId}") {
            val viewer = call.requireUserId()
            val storyId = call.pathUuid("storyId")
            DatabaseFactory.query {
                val author = Stories.selectAll().where { Stories.id eq storyId }
                    .firstOrNull()?.get(Stories.authorId) ?: throw notFound("Story not found")
                if (author != viewer) throw forbidden()
                StoryViews.deleteWhere { StoryViews.storyId eq storyId }
                StoryReactions.deleteWhere { StoryReactions.storyId eq storyId }
                Stories.deleteWhere { Stories.id eq storyId }
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun statusFor(row: org.jetbrains.exposed.sql.ResultRow): String {
    val viewers = row[Stories.liveViewerCount]
    return when {
        viewers > 0 -> "LIVE"
        row[Stories.caption].startsWith("PREMIERE:") -> "PREMIERE"
        else -> "NONE"
    }
}
