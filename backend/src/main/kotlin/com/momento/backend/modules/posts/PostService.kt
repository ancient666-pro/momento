package com.momento.backend.modules.posts

import com.momento.backend.common.PAGE_SIZE
import com.momento.backend.common.PostDto
import com.momento.backend.database.Comments
import com.momento.backend.database.MediaAssets
import com.momento.backend.database.PostBookmarks
import com.momento.backend.database.PostLikes
import com.momento.backend.database.PostMedia
import com.momento.backend.database.PostShares
import com.momento.backend.database.Posts
import com.momento.backend.database.Users
import com.momento.backend.modules.users.UserService
import java.util.UUID
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.selectAll

data class PostMediaInfo(
    val url: String,
    val type: String,
    val aspectRatio: Float,
    val durationSeconds: Int?
)

object PostService {

    fun mediaFor(postIds: Collection<UUID>): Map<UUID, PostMediaInfo> {
        if (postIds.isEmpty()) return emptyMap()
        val mediaRows = PostMedia.selectAll()
            .where { PostMedia.postId inList postIds.toList() }
            .orderBy(PostMedia.position, SortOrder.ASC)
            .toList()
        val assetIds = mediaRows.map { it[PostMedia.mediaAssetId] }.distinct()
        val assets = if (assetIds.isEmpty()) emptyMap() else MediaAssets.selectAll()
            .where { MediaAssets.id inList assetIds }
            .associateBy { it[MediaAssets.id] }

        return mediaRows.groupBy { it[PostMedia.postId] }.mapNotNull { (postId, rows) ->
            val first = rows.first()
            val asset = assets[first[PostMedia.mediaAssetId]]
            val url = asset?.get(MediaAssets.url) ?: return@mapNotNull null
            postId to PostMediaInfo(
                url = url,
                type = first[PostMedia.mediaType].uppercase(),
                aspectRatio = first[PostMedia.aspectRatio] ?: 1f,
                durationSeconds = first[PostMedia.durationSeconds]
            )
        }.toMap()
    }

    fun engagement(postIds: Collection<UUID>): Engagement {
        if (postIds.isEmpty()) return Engagement()
        val likes = PostLikes.select(PostLikes.postId, PostLikes.postId.count())
            .where { PostLikes.postId inList postIds.toList() }
            .groupBy(PostLikes.postId)
            .associate { it[PostLikes.postId] to it[PostLikes.postId.count()] }
        val comments = Comments.select(Comments.postId, Comments.postId.count())
            .where { (Comments.postId inList postIds.toList()) and Comments.deletedAt.isNull() }
            .groupBy(Comments.postId)
            .associate { it[Comments.postId] to it[Comments.postId.count()] }
        val shares = PostShares.select(PostShares.postId, PostShares.postId.count())
            .where { PostShares.postId inList postIds.toList() }
            .groupBy(PostShares.postId)
            .associate { it[PostShares.postId] to it[PostShares.postId.count()] }
        return Engagement(likes, comments, shares)
    }

    data class Engagement(
        val likes: Map<UUID, Long> = emptyMap(),
        val comments: Map<UUID, Long> = emptyMap(),
        val shares: Map<UUID, Long> = emptyMap()
    )

    fun viewerState(postIds: Collection<UUID>, viewerId: UUID?): ViewerState {
        if (viewerId == null || postIds.isEmpty()) return ViewerState()
        val ids = postIds.toList()
        val liked = PostLikes.select(PostLikes.postId)
            .where { (PostLikes.userId eq viewerId) and (PostLikes.postId inList ids) }
            .map { it[PostLikes.postId] }.toSet()
        val bookmarked = PostBookmarks.select(PostBookmarks.postId)
            .where { (PostBookmarks.userId eq viewerId) and (PostBookmarks.postId inList ids) }
            .map { it[PostBookmarks.postId] }.toSet()
        return ViewerState(liked, bookmarked)
    }

    data class ViewerState(
        val liked: Set<UUID> = emptySet(),
        val bookmarked: Set<UUID> = emptySet()
    )

    /**
     * Loads a page of posts matching [predicate], mapping authors media and
     * engagement in batches (no per-post queries).
     */
    fun page(
        predicate: Op<Boolean>,
        viewerId: UUID?,
        page: Int,
        orderBy: org.jetbrains.exposed.sql.Expression<*> = Posts.createdAt
    ): List<PostDto> {
        val rows = Posts.selectAll()
            .where { predicate and Posts.deletedAt.isNull() and (Posts.visibility eq "public") }
            .orderBy(orderBy, SortOrder.DESC)
            .limit(PAGE_SIZE).offset((page * PAGE_SIZE).toLong())
            .toList()
        return mapRows(rows, viewerId)
    }

    fun mapRows(rows: List<ResultRow>, viewerId: UUID?): List<PostDto> {
        if (rows.isEmpty()) return emptyList()
        val ids = rows.map { it[Posts.id] }
        val authorIds = rows.map { it[Posts.authorId] }.distinct()
        val authors = UserService.dtosByIds(authorIds)
        val media = mediaFor(ids)
        val engagement = engagement(ids)
        val state = viewerState(ids, viewerId)

        return rows.mapNotNull { row ->
            val id = row[Posts.id]
            val author = authors[row[Posts.authorId]] ?: return@mapNotNull null
            val m = media[id]
            PostDto(
                id = id.toString(),
                author = author,
                mediaUrl = m?.url ?: "",
                mediaType = m?.type ?: "IMAGE",
                caption = row[Posts.caption],
                likeCount = engagement.likes[id] ?: 0,
                commentCount = engagement.comments[id] ?: 0,
                shareCount = engagement.shares[id] ?: 0,
                isLiked = id in state.liked,
                isBookmarked = id in state.bookmarked,
                createdAtMillis = row[Posts.createdAt].toEpochMilli(),
                mediaAspectRatio = m?.aspectRatio ?: 1f,
                videoDurationSeconds = m?.durationSeconds
            )
        }
    }

    fun authorIdOf(postId: UUID): UUID? = Posts.select(Posts.authorId)
        .where { Posts.id eq postId }
        .firstOrNull()?.get(Posts.authorId)

    fun userSummaryIds(rows: List<ResultRow>): List<UUID> =
        rows.map { it[Posts.authorId] }.distinct()

    fun isPostVisible(postId: UUID): Boolean = Posts.selectAll()
        .where { (Posts.id eq postId) and Posts.deletedAt.isNull() }
        .count() > 0

    fun userPosts(userId: UUID, page: Int, viewerId: UUID?): List<PostDto> =
        page(Posts.authorId eq userId, viewerId, page)

    fun postIdsByKind(kind: String, page: Int = 0, limit: Int = PAGE_SIZE): List<UUID> =
        Posts.select(Posts.id)
            .where { (Posts.kind eq kind) and Posts.deletedAt.isNull() }
            .orderBy(Posts.createdAt, SortOrder.DESC)
            .limit(limit).offset((page * limit).toLong())
            .map { it[Posts.id] }

    fun postsByIds(ids: List<UUID>, viewerId: UUID?): List<PostDto> {
        if (ids.isEmpty()) return emptyList()
        val rows = Posts.selectAll().where { Posts.id inList ids }.toList()
        val byId = mapRows(rows, viewerId).associateBy { it.id }
        return ids.mapNotNull { byId[it.toString()] }
    }

    fun allViewsHidden(): Boolean = false
}
