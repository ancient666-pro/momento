package com.momento.backend.modules.posts

import com.momento.backend.common.CommentDto
import com.momento.backend.common.badRequest
import com.momento.backend.common.currentUserIdOrNull
import com.momento.backend.common.forbidden
import com.momento.backend.common.notFound
import com.momento.backend.common.pageParam
import com.momento.backend.common.pathUuid
import com.momento.backend.common.paged
import com.momento.backend.common.requireUserId
import com.momento.backend.database.Comments
import com.momento.backend.database.DatabaseFactory
import com.momento.backend.database.Hashtags
import com.momento.backend.database.MediaAssets
import com.momento.backend.database.PostBookmarks
import com.momento.backend.database.PostHashtags
import com.momento.backend.database.PostLikes
import com.momento.backend.database.PostMedia
import com.momento.backend.database.PostShares
import com.momento.backend.database.Posts
import com.momento.backend.modules.users.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

@Serializable
data class CreatePostRequest(
    val caption: String = "",
    val mediaUrl: String? = null,
    val mediaId: String? = null,
    val mediaType: String = "IMAGE",
    val aspectRatio: Float = 1f,
    val durationSeconds: Int? = null,
    val hashtag: String? = null,
    val kind: String = "post",
    val visibility: String = "public"
)

@Serializable
data class UpdatePostRequest(val caption: String? = null, val visibility: String? = null)

@Serializable
data class CreateCommentRequest(val body: String, val parentId: String? = null)

fun Route.postsModule() {
    route("/posts") {
        post {
            val userId = call.requireUserId()
            val body = call.receive<CreatePostRequest>()
            if (body.caption.length > 2200) throw badRequest("Caption is too long")
            if (body.mediaUrl == null && body.mediaId == null) throw badRequest("A post needs media")
            val mediaType = body.mediaType.uppercase()
            if (mediaType !in setOf("IMAGE", "VIDEO")) throw badRequest("Unsupported media type")
            if (body.visibility !in setOf("public", "followers", "private")) throw badRequest("Invalid visibility")
            if (body.kind !in setOf("post", "reel", "essay")) throw badRequest("Invalid post kind")

            val postId = DatabaseFactory.query {
                val postId = UUID.randomUUID()
                Posts.insert {
                    it[Posts.id] = postId
                    it[Posts.authorId] = userId
                    it[Posts.caption] = body.caption.trim()
                    it[Posts.visibility] = body.visibility
                    it[Posts.kind] = body.kind
                    it[Posts.hashtag] = body.hashtag?.trim()?.takeIf { h -> h.isNotBlank() }
                    it[Posts.createdAt] = Instant.now()
                    it[Posts.updatedAt] = Instant.now()
                }
                val assetId = body.mediaId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                if (assetId != null) {
                    val owned = MediaAssets.selectAll()
                        .where { (MediaAssets.id eq assetId) and (MediaAssets.ownerId eq userId) }
                        .count() > 0
                    if (!owned) throw badRequest("Unknown media asset")
                } else {
                    val url = body.mediaUrl!!.trim()
                    if (!url.startsWith("https://") && !url.startsWith("http://")) {
                        throw badRequest("Media URL must be an http(s) URL")
                    }
                    val newAssetId = UUID.randomUUID()
                    MediaAssets.insert {
                        it[MediaAssets.id] = newAssetId
                        it[MediaAssets.ownerId] = userId
                        it[MediaAssets.bucket] = "external"
                        it[MediaAssets.objectKey] = url
                        it[MediaAssets.url] = url
                        it[MediaAssets.mimeType] = if (mediaType == "VIDEO") "video/mp4" else "image/jpeg"
                        it[MediaAssets.sizeBytes] = 0
                        it[MediaAssets.status] = "ready"
                    }
                    PostMedia.insert {
                        it[PostMedia.postId] = postId
                        it[PostMedia.mediaAssetId] = newAssetId
                        it[PostMedia.mediaType] = mediaType.lowercase()
                        it[PostMedia.position] = 0
                        it[PostMedia.aspectRatio] = body.aspectRatio.coerceIn(0.4f, 2.5f)
                        it[PostMedia.durationSeconds] = body.durationSeconds
                    }
                }
                body.hashtag?.trim()?.removePrefix("#")?.takeIf { it.isNotBlank() }?.let { tag ->
                    attachHashtag(postId, tag.lowercase())
                }
                postId
            }

            val dto = DatabaseFactory.query {
                PostService.postsByIds(listOf(postId), userId).firstOrNull()
            } ?: throw notFound("Post not found")
            call.respond(HttpStatusCode.Created, dto)
        }

        get("/{postId}") {
            val postId = call.pathUuid("postId")
            val viewer = call.currentUserIdOrNull()
            val dto = DatabaseFactory.query { PostService.postsByIds(listOf(postId), viewer).firstOrNull() }
                ?: throw notFound("Post not found")
            call.respond(dto)
        }

        patch("/{postId}") {
            val userId = call.requireUserId()
            val postId = call.pathUuid("postId")
            val body = call.receive<UpdatePostRequest>()
            DatabaseFactory.query {
                val author = PostService.authorIdOf(postId) ?: throw notFound("Post not found")
                if (author != userId) throw forbidden()
                Posts.update({ Posts.id eq postId }) {
                    body.caption?.let { c -> it[Posts.caption] = c.take(2200) }
                    body.visibility?.let { v ->
                        if (v !in setOf("public", "followers", "private")) throw badRequest("Invalid visibility")
                        it[Posts.visibility] = v
                    }
                    it[Posts.updatedAt] = Instant.now()
                }
            }
            val dto = DatabaseFactory.query { PostService.postsByIds(listOf(postId), userId).firstOrNull() }
                ?: throw notFound("Post not found")
            call.respond(dto)
        }

        delete("/{postId}") {
            val userId = call.requireUserId()
            val postId = call.pathUuid("postId")
            DatabaseFactory.query {
                val author = PostService.authorIdOf(postId) ?: throw notFound("Post not found")
                if (author != userId) throw forbidden()
                Posts.update({ Posts.id eq postId }) { it[deletedAt] = Instant.now() }
            }
            call.respond(HttpStatusCode.NoContent)
        }

        post("/{postId}/like") {
            val userId = call.requireUserId()
            val postId = call.pathUuid("postId")
            DatabaseFactory.query {
                if (!PostService.isPostVisible(postId)) throw notFound("Post not found")
                val exists = PostLikes.selectAll()
                    .where { (PostLikes.postId eq postId) and (PostLikes.userId eq userId) }
                    .count() > 0
                if (!exists) {
                    PostLikes.insert {
                        it[PostLikes.postId] = postId
                        it[PostLikes.userId] = userId
                        it[createdAt] = Instant.now()
                    }
                }
            }
            call.respond(HttpStatusCode.NoContent)
        }

        delete("/{postId}/like") {
            val userId = call.requireUserId()
            val postId = call.pathUuid("postId")
            DatabaseFactory.query {
                PostLikes.deleteWhere { (PostLikes.postId eq postId) and (PostLikes.userId eq userId) }
            }
            call.respond(HttpStatusCode.NoContent)
        }

        post("/{postId}/bookmark") {
            val userId = call.requireUserId()
            val postId = call.pathUuid("postId")
            DatabaseFactory.query {
                if (!PostService.isPostVisible(postId)) throw notFound("Post not found")
                val exists = PostBookmarks.selectAll()
                    .where { (PostBookmarks.postId eq postId) and (PostBookmarks.userId eq userId) }
                    .count() > 0
                if (!exists) {
                    PostBookmarks.insert {
                        it[PostBookmarks.postId] = postId
                        it[PostBookmarks.userId] = userId
                        it[createdAt] = Instant.now()
                    }
                }
            }
            call.respond(HttpStatusCode.NoContent)
        }

        delete("/{postId}/bookmark") {
            val userId = call.requireUserId()
            val postId = call.pathUuid("postId")
            DatabaseFactory.query {
                PostBookmarks.deleteWhere { (PostBookmarks.postId eq postId) and (PostBookmarks.userId eq userId) }
            }
            call.respond(HttpStatusCode.NoContent)
        }

        post("/{postId}/share") {
            val userId = call.requireUserId()
            val postId = call.pathUuid("postId")
            DatabaseFactory.query {
                if (!PostService.isPostVisible(postId)) throw notFound("Post not found")
                PostShares.insert {
                    it[PostShares.postId] = postId
                    it[PostShares.userId] = userId
                    it[createdAt] = Instant.now()
                }
            }
            call.respond(HttpStatusCode.NoContent)
        }

        post("/{postId}/comments") {
            val userId = call.requireUserId()
            val postId = call.pathUuid("postId")
            val body = call.receive<CreateCommentRequest>()
            if (body.body.isBlank()) throw badRequest("Comment cannot be empty")
            if (body.body.length > 1000) throw badRequest("Comment is too long")
            val parentId = body.parentId?.let {
                runCatching { UUID.fromString(it) }.getOrElse { throw badRequest("Invalid parent comment") }
            }
            val commentId = DatabaseFactory.query {
                if (!PostService.isPostVisible(postId)) throw notFound("Post not found")
                if (parentId != null) {
                    val parentValid = Comments.selectAll()
                        .where { (Comments.id eq parentId) and (Comments.postId eq postId) }
                        .count() > 0
                    if (!parentValid) throw badRequest("Parent comment not found")
                }
                val id = UUID.randomUUID()
                Comments.insert {
                    it[Comments.id] = id
                    it[Comments.postId] = postId
                    it[Comments.authorId] = userId
                    it[Comments.parentId] = parentId
                    it[Comments.body] = body.body.trim()
                    it[Comments.createdAt] = Instant.now()
                }
                id
            }
            val dto = DatabaseFactory.query {
                val row = Comments.selectAll().where { Comments.id eq commentId }.first()
                val author = UserService.dtoById(userId) ?: throw notFound("Author not found")
                CommentDto(
                    id = commentId.toString(),
                    author = author,
                    body = row[Comments.body],
                    createdAtMillis = row[Comments.createdAt].toEpochMilli(),
                    parentId = parentId?.toString()
                )
            }
            call.respond(HttpStatusCode.Created, dto)
        }

        get("/{postId}/comments") {
            val postId = call.pathUuid("postId")
            val page = call.pageParam()
            val items = DatabaseFactory.query {
                val rows = Comments.selectAll()
                    .where { (Comments.postId eq postId) and Comments.deletedAt.isNull() }
                    .orderBy(Comments.createdAt, org.jetbrains.exposed.sql.SortOrder.ASC)
                    .limit(com.momento.backend.common.PAGE_SIZE).offset((page * com.momento.backend.common.PAGE_SIZE).toLong())
                    .toList()
                val authors = UserService.dtosByIds(rows.map { it[Comments.authorId] })
                rows.map { row ->
                    CommentDto(
                        id = row[Comments.id].toString(),
                        author = authors[row[Comments.authorId]]
                            ?: throw notFound("Comment author not found"),
                        body = row[Comments.body],
                        createdAtMillis = row[Comments.createdAt].toEpochMilli(),
                        parentId = row[Comments.parentId]?.toString()
                    )
                }
            }
            call.respond(paged(items, page))
        }
    }
}

private fun attachHashtag(postId: UUID, tag: String) {
    val existing = Hashtags.selectAll().where { Hashtags.name eq tag }.firstOrNull()
    val hashtagId = if (existing != null) {
        existing[Hashtags.id]
    } else {
        val id = UUID.randomUUID()
        Hashtags.insert {
            it[Hashtags.id] = id
            it[Hashtags.name] = tag
            it[Hashtags.postCount] = 0
        }
        id
    }
    Hashtags.update({ Hashtags.id eq hashtagId }) {
        it[Hashtags.postCount] = Hashtags.postCount + 1
    }
    PostHashtags.insert {
        it[PostHashtags.postId] = postId
        it[PostHashtags.hashtagId] = hashtagId
    }
}
