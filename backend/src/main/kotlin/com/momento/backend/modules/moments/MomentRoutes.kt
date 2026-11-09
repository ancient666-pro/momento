package com.momento.backend.modules.moments

import com.momento.backend.common.MomentDto
import com.momento.backend.common.badRequest
import com.momento.backend.common.forbidden
import com.momento.backend.common.notFound
import com.momento.backend.common.pathUuid
import com.momento.backend.common.requireUserId
import com.momento.backend.database.DatabaseFactory
import com.momento.backend.database.MediaAssets
import com.momento.backend.database.Moments
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
import java.util.UUID
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

@Serializable
data class CreateMomentRequest(
    val mediaUrl: String,
    val thumbnailUrl: String? = null,
    val caption: String = "",
    val durationSeconds: Int = 15
)

fun Route.momentsModule() {
    route("/moments") {
        post {
            val userId = call.requireUserId()
            val body = call.receive<CreateMomentRequest>()
            if (!body.mediaUrl.startsWith("http")) throw badRequest("Media URL must be an http(s) URL")
            if (body.durationSeconds !in 1..180) throw badRequest("Invalid moment duration")
            val id = DatabaseFactory.query {
                val assetId = UUID.randomUUID()
                MediaAssets.insert {
                    it[MediaAssets.id] = assetId
                    it[MediaAssets.ownerId] = userId
                    it[MediaAssets.bucket] = "external"
                    it[MediaAssets.objectKey] = body.mediaUrl
                    it[MediaAssets.url] = body.mediaUrl
                    it[MediaAssets.mimeType] = "video/mp4"
                    it[MediaAssets.sizeBytes] = 0
                    it[MediaAssets.status] = "ready"
                }
                val momentId = UUID.randomUUID()
                Moments.insert {
                    it[Moments.id] = momentId
                    it[Moments.authorId] = userId
                    it[Moments.mediaAssetId] = assetId
                    it[Moments.caption] = body.caption.take(200)
                    it[Moments.thumbnailUrl] = body.thumbnailUrl ?: body.mediaUrl
                    it[Moments.durationSeconds] = body.durationSeconds
                    it[Moments.createdAt] = Instant.now()
                }
                momentId
            }
            call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
        }

        get("/{momentId}") {
            val momentId = call.pathUuid("momentId")
            val dto = DatabaseFactory.query {
                val row = Moments.selectAll().where { Moments.id eq momentId }.firstOrNull()
                    ?: throw notFound("Moment not found")
                MomentDto(
                    id = momentId.toString(),
                    author = UserService.dtoById(row[Moments.authorId]) ?: throw notFound("Author not found"),
                    thumbnailUrl = row[Moments.thumbnailUrl] ?: "",
                    viewCount = row[Moments.viewCount],
                    durationSeconds = row[Moments.durationSeconds] ?: 0
                )
            }
            call.respond(dto)
        }

        delete("/{momentId}") {
            val userId = call.requireUserId()
            val momentId = call.pathUuid("momentId")
            DatabaseFactory.query {
                val author = Moments.selectAll().where { Moments.id eq momentId }
                    .firstOrNull()?.get(Moments.authorId) ?: throw notFound("Moment not found")
                if (author != userId) throw forbidden()
                Moments.deleteWhere { Moments.id eq momentId }
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
