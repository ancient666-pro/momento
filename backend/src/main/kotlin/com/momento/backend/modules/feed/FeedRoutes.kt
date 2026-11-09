package com.momento.backend.modules.feed

import com.momento.backend.common.PAGE_SIZE
import com.momento.backend.common.PostDto
import com.momento.backend.common.currentUserIdOrNull
import com.momento.backend.common.pageParam
import com.momento.backend.common.paged
import com.momento.backend.database.DatabaseFactory
import com.momento.backend.database.PostLikes
import com.momento.backend.database.Posts
import com.momento.backend.modules.posts.PostService
import com.momento.backend.modules.users.UserService
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.math.ln
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll

fun Route.feedModule() {
    route("/feed") {
        get("/for-you") {
            val viewer = call.currentUserIdOrNull()
            val page = call.pageParam()
            val result = DatabaseFactory.query { rankForYou(viewer, page) }
            call.respond(paged(result, page))
        }

        get("/following") {
            val viewer = call.currentUserIdOrNull()
            val page = call.pageParam()
            if (viewer == null) {
                call.respond(paged(emptyList<PostDto>(), page))
                return@get
            }
            val result = DatabaseFactory.query {
                val following = UserService.followingIds(viewer)
                if (following.isEmpty()) return@query emptyList()
                val rows = Posts.selectAll()
                    .where { (Posts.authorId inList following.toList()) and Posts.deletedAt.isNull() }
                    .orderBy(Posts.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
                    .limit(PAGE_SIZE).offset((page * PAGE_SIZE).toLong())
                    .toList()
                PostService.mapRows(rows, viewer)
            }
            call.respond(paged(result, page))
        }
    }
}

/**
 * Ranking uses observable signals only: recency, likes/comments/shares, whether
 * the viewer follows the author, and whether the viewer has previously engaged
 * with the author's content. No fabricated confidence scores are exposed.
 */
private fun rankForYou(viewerId: UUID?, page: Int): List<PostDto> {
    val candidates = Posts.selectAll()
        .where { Posts.deletedAt.isNull() and (Posts.visibility eq "public") }
        .orderBy(Posts.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
        .limit(300)
        .toList()
    if (candidates.isEmpty()) return emptyList()

    val ids = candidates.map { it[Posts.id] }
    val engagement = PostService.engagement(ids)
    val following = viewerId?.let { UserService.followingIds(it) }.orEmpty()
    val previouslyLikedAuthors: Set<UUID> = if (viewerId == null) {
        emptySet()
    } else {
        Posts.join(PostLikes, JoinType.INNER, Posts.id, PostLikes.postId)
            .select(Posts.authorId)
            .where { PostLikes.userId eq viewerId }
            .map { it[Posts.authorId] }
            .toSet()
    }

    val now = Instant.now()
    val ranked = candidates.sortedByDescending { post ->
        val id = post[Posts.id]
        val ageHours = Duration.between(post[Posts.createdAt], now).toMinutes() / 60.0
        val recency = 24.0 / (ageHours + 2.0)
        val likes = engagement.likes[id] ?: 0
        val comments = engagement.comments[id] ?: 0
        val shares = engagement.shares[id] ?: 0
        val engagementScore = ln(1.0 + likes * 3.0 + comments * 2.0 + shares * 4.0)
        val followBoost = if (post[Posts.authorId] in following) 6.0 else 0.0
        val affinityBoost = if (post[Posts.authorId] in previouslyLikedAuthors) 2.0 else 0.0
        recency + engagementScore + followBoost + affinityBoost
    }

    val offset = page * PAGE_SIZE
    if (offset >= ranked.size) return emptyList()
    val slice = ranked.subList(offset, minOf(offset + PAGE_SIZE, ranked.size))
    return PostService.mapRows(slice, viewerId)
}
