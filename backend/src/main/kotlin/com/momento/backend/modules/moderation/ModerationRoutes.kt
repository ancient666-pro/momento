package com.momento.backend.modules.moderation

import com.momento.backend.common.badRequest
import com.momento.backend.common.pageParam
import com.momento.backend.common.paged
import com.momento.backend.common.requireUserId
import com.momento.backend.database.ContentReports
import com.momento.backend.database.DatabaseFactory
import com.momento.backend.database.UserReports
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

@Serializable
data class CreateReportRequest(
    val targetType: String,
    val targetId: String,
    val reason: String,
    val details: String? = null
)

@Serializable
data class ReportDto(
    val id: String,
    val targetType: String,
    val targetId: String,
    val reason: String,
    val status: String,
    val createdAtMillis: Long
)

private val reportableTypes = setOf("user", "post", "comment", "story", "moment", "pod", "message")

fun Route.moderationModule() {
    route("/reports") {
        post {
            val reporter = call.requireUserId()
            val body = call.receive<CreateReportRequest>()
            if (body.targetType !in reportableTypes) throw badRequest("Unsupported report target")
            if (body.reason.isBlank()) throw badRequest("A reason is required")
            val targetId = runCatching { UUID.fromString(body.targetId) }
                .getOrElse { throw badRequest("Invalid target id") }
            val id = DatabaseFactory.query {
                val reportId = UUID.randomUUID()
                if (body.targetType == "user") {
                    UserReports.insert {
                        it[UserReports.id] = reportId
                        it[reporterId] = reporter
                        it[targetUserId] = targetId
                        it[reason] = body.reason.take(120)
                        it[details] = body.details?.take(2000)
                        it[status] = "open"
                        it[createdAt] = Instant.now()
                    }
                } else {
                    ContentReports.insert {
                        it[ContentReports.id] = reportId
                        it[ContentReports.reporterId] = reporter
                        it[ContentReports.targetType] = body.targetType
                        it[ContentReports.targetId] = targetId
                        it[ContentReports.reason] = body.reason.take(120)
                        it[ContentReports.details] = body.details?.take(2000)
                        it[ContentReports.status] = "open"
                        it[ContentReports.createdAt] = Instant.now()
                    }
                }
                reportId
            }
            call.respond(HttpStatusCode.Accepted, mapOf("id" to id.toString()))
        }

        get {
            val reporter = call.requireUserId()
            val page = call.pageParam()
            val items = DatabaseFactory.query {
                ContentReports.selectAll()
                    .where { ContentReports.reporterId eq reporter }
                    .orderBy(ContentReports.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
                    .limit(com.momento.backend.common.PAGE_SIZE).offset((page * com.momento.backend.common.PAGE_SIZE).toLong())
                    .map { row ->
                        ReportDto(
                            id = row[ContentReports.id].toString(),
                            targetType = row[ContentReports.targetType],
                            targetId = row[ContentReports.targetId].toString(),
                            reason = row[ContentReports.reason],
                            status = row[ContentReports.status],
                            createdAtMillis = row[ContentReports.createdAt].toEpochMilli()
                        )
                    }
            }
            call.respond(paged(items, page))
        }
    }
}
