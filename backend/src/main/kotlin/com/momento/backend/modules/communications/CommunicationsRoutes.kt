package com.momento.backend.modules.communications

import com.momento.backend.common.CallHistoryDto
import com.momento.backend.common.NexconnTokenDto
import com.momento.backend.common.badRequest
import com.momento.backend.common.notFound
import com.momento.backend.common.pageParam
import com.momento.backend.common.paged
import com.momento.backend.common.pathUuid
import com.momento.backend.common.requireUserId
import com.momento.backend.config.AppConfig
import com.momento.backend.database.CallHistory
import com.momento.backend.database.DatabaseFactory
import com.momento.backend.database.DeviceTokens
import com.momento.backend.database.NexconnUserMappings
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
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll

@Serializable
data class RegisterDeviceRequest(val fcmToken: String, val deviceName: String? = null)

@Serializable
data class RecordCallRequest(
    val calleeId: String? = null,
    val kind: String = "audio",
    val status: String = "completed",
    val durationSeconds: Int? = null
)

@Serializable
data class ConversationSummaryDto(
    val id: String,
    val peerId: String? = null,
    val lastMessageAt: Long? = null
)

fun Route.communicationsModule() {
    route("/communications") {
        post("/nexconn/token") {
            val userId = call.requireUserId()
            val user = DatabaseFactory.query { UserService.dtoById(userId) }
                ?: throw notFound("Account not found")

            val mapping = DatabaseFactory.query {
                NexconnUserMappings.selectAll()
                    .where { NexconnUserMappings.userId eq userId }
                    .firstOrNull()
            }

            val issued = NexconnPlatformClient.issueAccessToken(user)

            DatabaseFactory.query {
                if (mapping == null) {
                    NexconnUserMappings.insert {
                        it[NexconnUserMappings.userId] = userId
                        it[NexconnUserMappings.nexconnUserId] = issued.userId
                        it[NexconnUserMappings.createdAt] = Instant.now()
                    }
                } else if (mapping[NexconnUserMappings.nexconnUserId] != issued.userId) {
                    throw badRequest("Stored Nexconn identity does not match the platform identity")
                }
            }

            call.respond(
                NexconnTokenDto(
                    userId = issued.userId,
                    accessToken = issued.accessToken,
                    appKey = AppConfig.nexconnAppId,
                    areaCode = AppConfig.nexconnAreaCode,
                    apiBaseUrl = AppConfig.nexconnWsBaseUrl,
                    expiresInSeconds = null
                )
            )
        }

        get("/conversations") {
            // Conversation state lives in the Nexconn SDK on the device. The server
            // only returns the locally stored conversation metadata it owns.
            call.requireUserId()
            call.respond(paged(emptyList<ConversationSummaryDto>(), 0))
        }

        get("/call-history") {
            val userId = call.requireUserId()
            val page = call.pageParam()
            val items = DatabaseFactory.query {
                CallHistory.selectAll()
                    .where { (CallHistory.callerId eq userId) or (CallHistory.calleeId eq userId) }
                    .orderBy(CallHistory.startedAt, org.jetbrains.exposed.sql.SortOrder.DESC)
                    .limit(com.momento.backend.common.PAGE_SIZE).offset((page * com.momento.backend.common.PAGE_SIZE).toLong())
                    .map { row ->
                        CallHistoryDto(
                            id = row[CallHistory.id].toString(),
                            callerId = row[CallHistory.callerId].toString(),
                            calleeId = row[CallHistory.calleeId]?.toString(),
                            kind = row[CallHistory.kind],
                            status = row[CallHistory.status],
                            durationSeconds = row[CallHistory.durationSeconds],
                            startedAtMillis = row[CallHistory.startedAt].toEpochMilli()
                        )
                    }
            }
            call.respond(paged(items, page))
        }

        post("/call-history") {
            val userId = call.requireUserId()
            val body = call.receive<RecordCallRequest>()
            if (body.kind !in setOf("audio", "video", "group")) throw badRequest("Invalid call kind")
            if (body.status !in setOf("completed", "missed", "rejected")) throw badRequest("Invalid call status")
            val calleeId = body.calleeId?.let {
                runCatching { UUID.fromString(it) }.getOrElse { throw badRequest("Invalid callee") }
            }
            val id = DatabaseFactory.query {
                val historyId = UUID.randomUUID()
                CallHistory.insert {
                    it[CallHistory.id] = historyId
                    it[CallHistory.callerId] = userId
                    it[CallHistory.calleeId] = calleeId
                    it[CallHistory.kind] = body.kind
                    it[CallHistory.status] = body.status
                    it[CallHistory.durationSeconds] = body.durationSeconds?.coerceAtLeast(0)
                    it[CallHistory.startedAt] = Instant.now()
                }
                historyId
            }
            call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
        }

        post("/devices") {
            val userId = call.requireUserId()
            val body = call.receive<RegisterDeviceRequest>()
            if (body.fcmToken.isBlank()) throw badRequest("FCM token is required")
            DatabaseFactory.query {
                val exists = DeviceTokens.selectAll()
                    .where { (DeviceTokens.userId eq userId) and (DeviceTokens.fcmToken eq body.fcmToken) }
                    .count() > 0
                if (!exists) {
                    DeviceTokens.insert {
                        it[DeviceTokens.userId] = userId
                        it[DeviceTokens.fcmToken] = body.fcmToken
                        it[DeviceTokens.deviceName] = body.deviceName
                        it[DeviceTokens.lastSeenAt] = Instant.now()
                    }
                }
            }
            call.respond(HttpStatusCode.NoContent)
        }

        delete("/devices/{deviceId}") {
            val userId = call.requireUserId()
            val deviceId = call.pathUuid("deviceId")
            DatabaseFactory.query {
                DeviceTokens.deleteWhere {
                    (DeviceTokens.id eq deviceId) and (DeviceTokens.userId eq userId)
                }
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
