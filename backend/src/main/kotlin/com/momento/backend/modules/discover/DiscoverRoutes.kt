package com.momento.backend.modules.discover

import com.momento.backend.common.AudioRoomDto
import com.momento.backend.common.NearbyProfileDto
import com.momento.backend.common.PodPollCardDto
import com.momento.backend.common.SparkCardDto
import com.momento.backend.common.UserDto
import com.momento.backend.common.badRequest
import com.momento.backend.common.currentUserIdOrNull
import com.momento.backend.common.notFound
import com.momento.backend.common.pageParam
import com.momento.backend.common.paged
import com.momento.backend.common.pathUuid
import com.momento.backend.common.requireUserId
import com.momento.backend.database.DatabaseFactory
import com.momento.backend.database.DiscoveryActions
import com.momento.backend.database.MediaAssets
import com.momento.backend.database.PodMembers
import com.momento.backend.database.PodSessions
import com.momento.backend.database.Pods
import com.momento.backend.database.Posts
import com.momento.backend.database.UserLocationData
import com.momento.backend.database.Users
import com.momento.backend.modules.posts.PostService
import com.momento.backend.modules.users.UserService
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.Instant
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll

@Serializable
data class DiscoverActionRequest(val target: String)

@Serializable
data class SearchResponseDto(
    val users: List<UserDto> = emptyList(),
    val posts: List<com.momento.backend.common.PostDto> = emptyList(),
    val hashtags: List<String> = emptyList(),
    val pods: List<AudioRoomDto> = emptyList()
)

fun Route.discoverModule() {
    route("/discover") {
        get("/people") {
            val viewer = call.requireUserId()
            val items = DatabaseFactory.query { nearbyPeople(viewer) }
            call.respond(paged(items, 0))
        }

        post("/pass") { recordAction(call, "pass") }
        post("/like") { recordAction(call, "like") }
        post("/super-like") { recordAction(call, "super_like") }

        get("/trending") {
            val filter = call.request.queryParameters["filter"]?.lowercase() ?: "trending"
            val viewer = call.currentUserIdOrNull()
            val page = call.pageParam()
            val items = DatabaseFactory.query { trendingSparks(filter, viewer, page) }
            call.respond(paged(items, page))
        }

        get("/search") {
            val query = call.request.queryParameters["q"]?.trim().orEmpty()
            if (query.length < 2) {
                call.respond(SearchResponseDto())
                return@get
            }
            val viewer = call.currentUserIdOrNull()
            val result = DatabaseFactory.query { search(query, viewer) }
            call.respond(result)
        }

        get("/hashtags/{hashtag}") {
            val tag = call.parameters["hashtag"]?.removePrefix("#")?.lowercase()
                ?: throw badRequest("Hashtag is required")
            val viewer = call.currentUserIdOrNull()
            val page = call.pageParam()
            val posts = DatabaseFactory.query {
                val rows = Posts.selectAll()
                    .where {
                        (Posts.hashtag eq tag) and Posts.deletedAt.isNull()
                    }
                    .orderBy(Posts.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
                    .limit(com.momento.backend.common.PAGE_SIZE).offset((page * com.momento.backend.common.PAGE_SIZE).toLong())
                    .toList()
                PostService.mapRows(rows, viewer)
            }
            call.respond(paged(posts, page))
        }

        get("/audio-rooms") {
            call.respond(paged(DatabaseFactory.query { liveAudioRooms() }, 0))
        }
    }
}

private suspend fun recordAction(
    call: io.ktor.server.application.ApplicationCall,
    action: String
) {
    val viewer = call.requireUserId()
    val body = call.receive<DiscoverActionRequest>()
    val target = runCatching { UUID.fromString(body.target) }.getOrElse { throw badRequest("Invalid target") }
    if (target == viewer) throw badRequest("You cannot act on your own profile")
    DatabaseFactory.query {
        val exists = Users.selectAll().where { Users.id eq target }.count() > 0
        if (!exists) throw notFound("Profile not found")
        val already = DiscoveryActions.selectAll()
            .where { (DiscoveryActions.actorId eq viewer) and (DiscoveryActions.targetId eq target) and (DiscoveryActions.action eq action) }
            .count() > 0
        if (!already) {
            DiscoveryActions.insert {
                it[DiscoveryActions.actorId] = viewer
                it[DiscoveryActions.targetId] = target
                it[DiscoveryActions.action] = action
                it[DiscoveryActions.createdAt] = Instant.now()
            }
        }
    }
    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

/**
 * Nearby discovery is opt-in. Exact coordinates are used only to compute an
 * approximate distance, which is rounded to the target's privacy band before
 * being returned.
 */
private fun nearbyPeople(viewer: UUID): List<NearbyProfileDto> {
    val origin = UserLocationData.selectAll()
        .where { UserLocationData.userId eq viewer }
        .firstOrNull()
    val myLat = origin?.get(UserLocationData.latitude) ?: return emptyList()
    val myLng = origin?.get(UserLocationData.longitude) ?: return emptyList()

    // Coordinates and identifiers are typed server-side values (Double/UUID), so they
    // are embedded directly into the spatial query instead of being interpolated
    // from client input.
    val sql = """
        SELECT u.id::text AS user_id,
               ST_Distance(
                   ST_SetSRID(ST_MakePoint(l.longitude, l.latitude), 4326)::geography,
                   ST_SetSRID(ST_MakePoint($myLng, $myLat), 4326)::geography
               ) / 1000.0 AS distance_km,
               l.privacy_band_km AS privacy_band_km,
               COALESCE(cardinality(ARRAY(
                   SELECT unnest(p1.interest_tags) INTERSECT SELECT unnest(p2.interest_tags)
               )), 0) AS shared_interests,
               COALESCE(cardinality(p1.interest_tags), 0) AS total_interests,
               p1.age AS age
        FROM user_location_data l
        JOIN users u ON u.id = l.user_id
        LEFT JOIN user_profiles p1 ON p1.user_id = u.id
        JOIN user_profiles p2 ON p2.user_id = '$viewer'::uuid
        WHERE u.deleted_at IS NULL
          AND u.id <> '$viewer'::uuid
          AND l.latitude IS NOT NULL
          AND l.longitude IS NOT NULL
          AND COALESCE(p2.interest_tags, '{}') <> '{}'
          AND NOT EXISTS (
              SELECT 1 FROM discovery_actions a
              WHERE a.actor_id = '$viewer'::uuid AND a.target_id = u.id
          )
          AND NOT EXISTS (
              SELECT 1 FROM blocks b
              WHERE (b.blocker_id = '$viewer'::uuid AND b.blocked_id = u.id)
                 OR (b.blocker_id = u.id AND b.blocked_id = '$viewer'::uuid)
          )
        ORDER BY distance_km ASC
        LIMIT 20
    """.trimIndent()

    val rows = ArrayList<NearbyRow>()
    org.jetbrains.exposed.sql.transactions.transaction {
        exec(sql) { rs ->
            while (rs.next()) {
                val id = UUID.fromString(rs.getString("user_id"))
                val distance = rs.getDouble("distance_km")
                val band = rs.getInt("privacy_band_km").coerceAtLeast(1)
                val shared = rs.getInt("shared_interests")
                val total = rs.getInt("total_interests")
                val ageValue = rs.getInt("age")
                rows.add(
                    NearbyRow(
                        userId = id,
                        distanceKm = distance,
                        bandKm = band,
                        sharedInterests = shared,
                        totalInterests = total,
                        age = if (rs.wasNull()) null else ageValue
                    )
                )
            }
        }
    }

    val users = UserService.dtosByIds(rows.map { it.userId })
    return rows.mapNotNull { row ->
        val user = users[row.userId] ?: return@mapNotNull null
        val approxDistance = maxOf(
            row.bandKm.toDouble(),
            ((row.distanceKm / row.bandKm).roundToInt() * row.bandKm).toDouble()
        )
        val matchPercent = if (row.totalInterests > 0 && row.sharedInterests > 0) {
            (row.sharedInterests * 100 / row.totalInterests).coerceIn(1, 99)
        } else {
            0
        }
        NearbyProfileDto(
            profile = user,
            age = row.age,
            distanceKm = approxDistance,
            matchPercent = matchPercent,
            interests = emptyList()
        )
    }
}

private data class NearbyRow(
    val userId: UUID,
    val distanceKm: Double,
    val bandKm: Int,
    val sharedInterests: Int,
    val totalInterests: Int,
    val age: Int?
)

private fun trendingSparks(filter: String, viewer: UUID?, page: Int): List<SparkCardDto> {
    val kind = when (filter) {
        "reels", "creative" -> "reel"
        "audio_session" -> null
        else -> "reel"
    }
    if (filter == "audio_session") {
        return liveAudioRooms().map { room ->
            SparkCardDto(
                id = room.id,
                title = room.title,
                author = room.hosts.firstOrNull() ?: UserDto(
                    id = "", displayName = "Pod", username = "pod"
                ),
                mediaUrl = room.artworkUrl.orEmpty(),
                viewCount = room.listenerCount,
                likeCount = 0,
                commentCount = 0
            )
        }
    }
    val rows = Posts.selectAll()
        .where {
            (Posts.kind eq (kind ?: "reel")) and Posts.deletedAt.isNull() and (Posts.visibility eq "public")
        }
        .orderBy(Posts.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
        .limit(120)
        .toList()
    val engagement = PostService.engagement(rows.map { it[Posts.id] })
    val ordered = if (filter == "new") rows else rows.sortedByDescending { row ->
        val id = row[Posts.id]
        (engagement.likes[id] ?: 0) * 3 + (engagement.comments[id] ?: 0) * 2 + (engagement.shares[id] ?: 0) * 4
    }
    val offset = page * com.momento.backend.common.PAGE_SIZE
    val slice = if (offset >= ordered.size) emptyList() else
        ordered.subList(offset, minOf(offset + com.momento.backend.common.PAGE_SIZE, ordered.size))
    val posts = PostService.mapRows(slice, viewer)
    return posts.mapNotNull { post ->
        if (post.mediaUrl.isBlank()) return@mapNotNull null
        SparkCardDto(
            id = post.id,
            title = post.caption,
            author = post.author,
            mediaUrl = post.mediaUrl,
            viewCount = post.likeCount + post.commentCount + post.shareCount,
            likeCount = post.likeCount,
            commentCount = post.commentCount,
            durationSeconds = post.videoDurationSeconds
        )
    }
}

private fun search(query: String, viewer: UUID?): SearchResponseDto {
    val pattern = "%${query.lowercase()}%"
    val userRows = Users.selectAll()
        .where {
            (Users.deletedAt.isNull()) and (
                (Users.username.lowerCase() like pattern) or (Users.displayName.lowerCase() like pattern)
                )
        }
        .limit(10)
        .toList()
    val users = UserService.dtosByIds(userRows.map { it[Users.id] }).values.toList()

    val postRows = Posts.selectAll()
        .where { (Posts.caption.lowerCase() like pattern) and Posts.deletedAt.isNull() }
        .limit(10)
        .toList()
    val posts = PostService.mapRows(postRows, viewer)

    val hashtags = com.momento.backend.database.Hashtags.selectAll()
        .where { com.momento.backend.database.Hashtags.name like pattern }
        .limit(10)
        .map { it[com.momento.backend.database.Hashtags.name] }

    val podRows = Pods.selectAll()
        .where { (Pods.title.lowerCase() like pattern) and Pods.deletedAt.isNull() }
        .limit(10)
        .toList()
    val pods = podRows.map { row ->
        AudioRoomDto(
            id = row[Pods.id].toString(),
            title = row[Pods.title],
            listenerCount = row[Pods.listenerCount],
            hosts = UserService.dtoById(row[Pods.hostId])?.let { listOf(it) } ?: emptyList(),
            artworkUrl = row[Pods.artworkUrl]
        )
    }

    return SearchResponseDto(users = users, posts = posts, hashtags = hashtags, pods = pods)
}

private fun liveAudioRooms(): List<AudioRoomDto> {
    val rows = Pods.selectAll()
        .where { (Pods.live eq true) and Pods.deletedAt.isNull() }
        .orderBy(Pods.listenerCount, org.jetbrains.exposed.sql.SortOrder.DESC)
        .limit(20)
        .toList()
    return rows.map { row ->
        val podId = row[Pods.id]
        val hosts = PodMembers.selectAll()
            .where { (PodMembers.podId eq podId) and (PodMembers.role inList listOf("host", "co_host")) }
            .map { it[PodMembers.userId] }
        AudioRoomDto(
            id = podId.toString(),
            title = row[Pods.title],
            listenerCount = row[Pods.listenerCount],
            hosts = UserService.dtosByIds(hosts).values.toList(),
            artworkUrl = row[Pods.artworkUrl]
        )
    }
}
