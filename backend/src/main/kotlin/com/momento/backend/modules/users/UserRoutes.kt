package com.momento.backend.modules.users

import com.momento.backend.common.MomentDto
import com.momento.backend.common.PageResponse
import com.momento.backend.common.PAGE_SIZE
import com.momento.backend.common.badRequest
import com.momento.backend.common.currentUserIdOrNull
import com.momento.backend.common.notFound
import com.momento.backend.common.pageParam
import com.momento.backend.common.pathUuid
import com.momento.backend.common.paged
import com.momento.backend.common.requireUserId
import com.momento.backend.database.Blocks
import com.momento.backend.database.DatabaseFactory
import com.momento.backend.database.Follows
import com.momento.backend.database.MediaAssets
import com.momento.backend.database.Moments
import com.momento.backend.database.UserLocationData
import com.momento.backend.database.UserProfiles
import com.momento.backend.database.Users
import com.momento.backend.modules.posts.PostService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val username: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val location: String? = null,
    val website: String? = null,
    val category: String? = null,
    val isPrivate: Boolean? = null
)

@Serializable
data class LocationRequest(
    val latitude: Double,
    val longitude: Double,
    val privacyBandKm: Int = 5
)

private val usernameRegex = Regex("^[a-z0-9_.]{3,30}$")

fun Route.usersModule() {
    route("/users") {
        get("/me") {
            val userId = call.requireUserId()
            val dto = DatabaseFactory.query { UserService.dtoById(userId) }
                ?: throw notFound("Account not found")
            call.respond(dto)
        }

        patch("/me") {
            val userId = call.requireUserId()
            val body = call.receive<UpdateProfileRequest>()
            DatabaseFactory.query {
                body.displayName?.let { name ->
                    if (name.isBlank()) throw badRequest("Display name cannot be empty")
                    Users.update({ Users.id eq userId }) {
                        it[Users.displayName] = name.trim()
                        it[Users.updatedAt] = Instant.now()
                    }
                }
                body.username?.let { username ->
                    val normalized = username.trim().lowercase()
                    if (!usernameRegex.matches(normalized)) {
                        throw badRequest("Username must be 3-30 characters: letters, numbers, underscore or dot")
                    }
                    val taken = Users.selectAll()
                        .where { (Users.username eq normalized) and (Users.id neq userId) }
                        .count() > 0
                    if (taken) throw com.momento.backend.common.conflict("That username is already taken", "username_taken")
                    Users.update({ Users.id eq userId }) {
                        it[Users.username] = normalized
                        it[Users.updatedAt] = Instant.now()
                    }
                }
                body.avatarUrl?.let { url ->
                    Users.update({ Users.id eq userId }) { it[Users.avatarUrl] = url }
                }
                if (body.bio != null || body.location != null || body.website != null ||
                    body.category != null || body.isPrivate != null
                ) {
                    ensureProfile(userId)
                    UserProfiles.update({ UserProfiles.userId eq userId }) { stmt ->
                        body.bio?.let { stmt[UserProfiles.bio] = it.take(280) }
                        body.location?.let { stmt[UserProfiles.location] = it.take(80) }
                        body.website?.let { stmt[UserProfiles.website] = it.take(200) }
                        body.category?.let { stmt[UserProfiles.category] = it.take(60) }
                        body.isPrivate?.let { stmt[UserProfiles.isPrivate] = it }
                        stmt[UserProfiles.updatedAt] = Instant.now()
                    }
                }
            }
            val dto = DatabaseFactory.query { UserService.dtoById(userId) }
                ?: throw notFound("Account not found")
            call.respond(dto)
        }

        put("/me/location") {
            val userId = call.requireUserId()
            val body = call.receive<LocationRequest>()
            if (body.latitude !in -90.0..90.0 || body.longitude !in -180.0..180.0) {
                throw badRequest("Invalid coordinates")
            }
            DatabaseFactory.query {
                val exists = UserLocationData.selectAll()
                    .where { UserLocationData.userId eq userId }.count() > 0
                if (exists) {
                    UserLocationData.update({ UserLocationData.userId eq userId }) {
                        it[latitude] = body.latitude
                        it[longitude] = body.longitude
                        it[privacyBandKm] = body.privacyBandKm.coerceIn(1, 50)
                        it[updatedAt] = Instant.now()
                    }
                } else {
                    UserLocationData.insert {
                        it[UserLocationData.userId] = userId
                        it[UserLocationData.latitude] = body.latitude
                        it[UserLocationData.longitude] = body.longitude
                        it[UserLocationData.privacyBandKm] = body.privacyBandKm.coerceIn(1, 50)
                        it[UserLocationData.updatedAt] = Instant.now()
                    }
                }
            }
            call.respond(HttpStatusCode.NoContent)
        }

        delete("/me/location") {
            val userId = call.requireUserId()
            DatabaseFactory.query {
                UserLocationData.deleteWhere { UserLocationData.userId eq userId }
            }
            call.respond(HttpStatusCode.NoContent)
        }

        get("/{userId}") {
            val target = call.userIdParamOrMe()
            val viewer = call.currentUserIdOrNull()
            val dto = DatabaseFactory.query {
                if (viewer != null && UserService.isBlocked(target, viewer)) {
                    throw notFound("Profile not found")
                }
                UserService.dtoById(target)
            } ?: throw notFound("Profile not found")
            call.respond(dto)
        }

        get("/{userId}/posts") {
            val target = call.userIdParamOrMe()
            val viewer = call.currentUserIdOrNull()
            val page = call.pageParam()
            val posts = DatabaseFactory.query { PostService.userPosts(target, page, viewer) }
            call.respond(paged(posts, page))
        }

        get("/{userId}/moments") {
            val target = call.userIdParamOrMe()
            val page = call.pageParam()
            val items = DatabaseFactory.query {
                val rows = Moments.selectAll()
                    .where { Moments.authorId eq target }
                    .orderBy(Moments.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
                    .limit(PAGE_SIZE).offset((page * PAGE_SIZE).toLong())
                    .toList()
                val author = UserService.dtoById(target) ?: throw notFound("Profile not found")
                rows.map { row ->
                    MomentDto(
                        id = row[Moments.id].toString(),
                        author = author,
                        thumbnailUrl = row[Moments.thumbnailUrl] ?: "",
                        viewCount = row[Moments.viewCount],
                        durationSeconds = row[Moments.durationSeconds] ?: 0
                    )
                }
            }
            call.respond(paged(items, page))
        }

        get("/{userId}/followers") {
            val target = call.userIdParamOrMe()
            val page = call.pageParam()
            call.respond(userList(Follows.followerId, Follows.followeeId, target, page))
        }

        get("/{userId}/following") {
            val target = call.userIdParamOrMe()
            val page = call.pageParam()
            call.respond(userList(Follows.followeeId, Follows.followerId, target, page))
        }

        post("/{userId}/follow") {
            val viewer = call.requireUserId()
            val target = call.userIdParamOrMe()
            if (viewer == target) throw badRequest("You cannot follow yourself")
            DatabaseFactory.query {
                val blocked = UserService.isBlocked(target, viewer) || UserService.isBlocked(viewer, target)
                if (blocked) throw notFound("Profile not found")
                val exists = Follows.selectAll()
                    .where { (Follows.followerId eq viewer) and (Follows.followeeId eq target) }
                    .count() > 0
                if (!exists) {
                    Follows.insert {
                        it[Follows.followerId] = viewer
                        it[Follows.followeeId] = target
                        it[Follows.createdAt] = Instant.now()
                    }
                }
            }
            call.respond(HttpStatusCode.NoContent)
        }

        delete("/{userId}/follow") {
            val viewer = call.requireUserId()
            val target = call.userIdParamOrMe()
            DatabaseFactory.query {
                Follows.deleteWhere { (Follows.followerId eq viewer) and (Follows.followeeId eq target) }
            }
            call.respond(HttpStatusCode.NoContent)
        }

        post("/{userId}/block") {
            val viewer = call.requireUserId()
            val target = call.userIdParamOrMe()
            if (viewer == target) throw badRequest("You cannot block yourself")
            DatabaseFactory.query {
                val exists = Blocks.selectAll()
                    .where { (Blocks.blockerId eq viewer) and (Blocks.blockedId eq target) }
                    .count() > 0
                if (!exists) {
                    Blocks.insert {
                        it[Blocks.blockerId] = viewer
                        it[Blocks.blockedId] = target
                        it[Blocks.createdAt] = Instant.now()
                    }
                }
                // Blocking also removes any follow edge in both directions.
                Follows.deleteWhere { (Follows.followerId eq viewer) and (Follows.followeeId eq target) }
                Follows.deleteWhere { (Follows.followerId eq target) and (Follows.followeeId eq viewer) }
            }
            call.respond(HttpStatusCode.NoContent)
        }

        delete("/{userId}/block") {
            val viewer = call.requireUserId()
            val target = call.userIdParamOrMe()
            DatabaseFactory.query {
                Blocks.deleteWhere { (Blocks.blockerId eq viewer) and (Blocks.blockedId eq target) }
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun userList(
    idColumn: org.jetbrains.exposed.sql.Column<UUID>,
    matchColumn: org.jetbrains.exposed.sql.Column<UUID>,
    target: UUID,
    page: Int
): PageResponse<com.momento.backend.common.UserDto> {
    val result = DatabaseFactory.query {
        val ids = Follows.select(idColumn)
            .where { matchColumn eq target }
            .limit(PAGE_SIZE).offset((page * PAGE_SIZE).toLong())
            .map { it[idColumn] }
        val dtos = UserService.dtosByIds(ids)
        ids.mapNotNull { dtos[it] }
    }
    return paged(result, page)
}

private fun ApplicationCall.userIdParamOrMe(): UUID =
    if (parameters["userId"] == "me") requireUserId() else pathUuid("userId")

private fun ensureProfile(userId: UUID) {
    val exists = UserProfiles.selectAll().where { UserProfiles.userId eq userId }.count() > 0
    if (!exists) {
        UserProfiles.insert {
            it[UserProfiles.userId] = userId
            it[UserProfiles.bio] = ""
        }
    }
}
